package com.msa4lmsv2academic.domain.professor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.professor.request.ProfessorSearchRequestDTO;
import com.msa4lmsv2academic.domain.professor.request.ProfessorUpdateRequestDTO;
import com.msa4lmsv2academic.domain.professor.response.ProfessorDetailResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.InvalidProfessorRequestException;
import com.msa4lmsv2academic.global.error.ProfessorAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProfessorManagementServiceTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9301L;
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_ID, "ADMIN");

    @Autowired
    private ProfessorManagementService professorManagementService;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Department computerScience;
    private Department artificialIntelligence;
    private Department inactiveDepartment;
    private Professor kimProfessor;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        professorRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();

        computerScience = departmentRepository.save(
                Department.create("205", null, "컴퓨터공학과", true)
        );
        artificialIntelligence = departmentRepository.save(
                Department.create("206", null, "인공지능학과", true)
        );
        inactiveDepartment = departmentRepository.save(
                Department.create("207", null, "폐지학과", false)
        );

        entityManager.persist(User.synchronize(
                ADMIN_ID, "교수관리자", "professor-admin@test.com", null, null,
                UserRole.ADMIN, UserStatus.ACTIVE
        ));
        kimProfessor = persistProfessor(
                9302L, "김교수", "Kim.Professor@test.com", UserStatus.ACTIVE,
                computerScience, (short) 2020
        );
        persistProfessor(
                9303L, "박교수", "park.professor@test.com", UserStatus.LOCKED,
                artificialIntelligence, (short) 2018
        );
        entityManager.flush();
    }

    @Test
    void searchFiltersNameOrEmailIgnoringCaseAndReturnsStatus() {
        PageRes<?> byName = professorManagementService.searchProfessors(
                new ProfessorSearchRequestDTO(1, 20, null, null, UserStatus.ACTIVE, "  김교수  "),
                ADMIN
        );
        PageRes<?> byEmail = professorManagementService.searchProfessors(
                new ProfessorSearchRequestDTO(1, 20, null, 2020, null, "KIM.PROFESSOR"),
                ADMIN
        );

        assertThat(byName.totalCount()).isEqualTo(1);
        assertThat(byName.items()).extracting("status").containsExactly(UserStatus.ACTIVE);
        assertThat(byEmail.totalCount()).isEqualTo(1);
        assertThat(byEmail.items()).extracting("professorId").containsExactly(kimProfessor.getId());
    }

    @Test
    void searchUsesNameThenProfessorIdOrderAndClampsPageSize() {
        persistProfessor(
                9304L, "김교수", "kim.second@test.com", UserStatus.INACTIVE,
                computerScience, (short) 2019
        );
        entityManager.flush();

        var result = professorManagementService.searchProfessors(
                new ProfessorSearchRequestDTO(1, 500, null, null, null, ""),
                ADMIN
        );

        assertThat(result.size()).isEqualTo(100);
        assertThat(result.items()).extracting("name").containsExactly("김교수", "김교수", "박교수");
        assertThat(result.items().get(0).professorId()).isEqualTo(kimProfessor.getId());
    }

    @Test
    void queryRejectsNonAdminWhenCalledOutsideController() {
        CurrentUser professor = new CurrentUser(9302L, "PROFESSOR");

        assertThatThrownBy(() -> professorManagementService.searchProfessors(
                new ProfessorSearchRequestDTO(1, 20, null, null, null, null),
                professor
        )).isInstanceOf(ProfessorAccessDeniedException.class);
        assertThatThrownBy(() -> professorManagementService.getProfessor(kimProfessor.getId(), professor))
                .isInstanceOf(ProfessorAccessDeniedException.class);
    }

    @Test
    void updateChangesEmploymentAndRecordsLimitedAuditSnapshot() {
        ProfessorDetailResponseDTO updated = professorManagementService.updateProfessor(
                kimProfessor.getId(),
                new ProfessorUpdateRequestDTO(artificialIntelligence.getId(), 2021, " 소속 변경 "),
                new CurrentUser(ADMIN_ID, "ADMIN"),
                "professor-update-request",
                "127.0.0.1"
        );

        assertThat(updated.departmentId()).isEqualTo(artificialIntelligence.getId());
        assertThat(updated.hireYear()).isEqualTo((short) 2021);
        assertThat(auditLogRepository.findAll()).singleElement().satisfies(this::assertAuditLog);
    }

    @Test
    void sameValuesReturnCurrentStateWithoutAuditLog() {
        ProfessorDetailResponseDTO current = professorManagementService.updateProfessor(
                kimProfessor.getId(),
                new ProfessorUpdateRequestDTO(computerScience.getId(), 2020, "재확인"),
                new CurrentUser(ADMIN_ID, "ADMIN"),
                null,
                null
        );

        assertThat(current.departmentId()).isEqualTo(computerScience.getId());
        assertThat(auditLogRepository.findAll()).isEmpty();
    }

    @Test
    void updateRejectsInactiveDepartmentAndFutureHireYear() {
        assertThatThrownBy(() -> professorManagementService.updateProfessor(
                kimProfessor.getId(),
                new ProfessorUpdateRequestDTO(inactiveDepartment.getId(), null, "소속 변경"),
                new CurrentUser(ADMIN_ID, "ADMIN"), null, null
        )).isInstanceOf(InvalidProfessorRequestException.class);

        assertThatThrownBy(() -> professorManagementService.updateProfessor(
                kimProfessor.getId(),
                new ProfessorUpdateRequestDTO(null, Year.now().getValue() + 1, "임용 연도 변경"),
                new CurrentUser(ADMIN_ID, "ADMIN"), null, null
        )).isInstanceOf(InvalidProfessorRequestException.class);
    }

    private Professor persistProfessor(Long userId, String name, String email, UserStatus status,
                                       Department department, Short hireYear) {
        User user = User.synchronize(
                userId, name, email, "010-1234-5678", "서울특별시",
                UserRole.PROFESSOR, status
        );
        entityManager.persist(user);
        return professorRepository.save(Professor.create(user, hireYear, department));
    }

    private void assertAuditLog(AuditLog log) {
        assertThat(log.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(log.getAction()).isEqualTo("PROFESSOR_EMPLOYMENT_UPDATE");
        assertThat(log.getTargetType()).isEqualTo("PROFESSOR");
        assertThat(log.getTargetId()).isEqualTo(kimProfessor.getId());
        assertThat(log.getReason()).isEqualTo("소속 변경");
        assertThat(log.getRequestId()).isEqualTo("professor-update-request");
        assertThat(log.getBeforeValue()).containsOnlyKeys("departmentId", "departmentName", "hireYear");
        assertThat(log.getAfterValue()).containsOnlyKeys("departmentId", "departmentName", "hireYear");
        assertThat(log.getAfterValue()).containsEntry("departmentName", "인공지능학과");
    }
}
