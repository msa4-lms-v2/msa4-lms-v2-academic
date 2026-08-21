package com.msa4lmsv2academic.domain.academicschedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleRepository;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleCreateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleSearchRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleStatusRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.request.AcademicScheduleUpdateRequestDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleDetailResponseDTO;
import com.msa4lmsv2academic.domain.academicschedule.response.AcademicScheduleSummaryResponseDTO;
import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.AcademicScheduleAccessDeniedException;
import com.msa4lmsv2academic.global.error.DuplicateAcademicScheduleException;
import com.msa4lmsv2academic.global.error.InvalidAcademicScheduleRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AcademicScheduleServiceTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9701L;
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_ID, "ADMIN");
    private static final CurrentUser STUDENT = new CurrentUser(9702L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(9703L, "PROFESSOR");

    @Autowired
    private AcademicScheduleService academicScheduleService;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        academicScheduleRepository.deleteAllInBatch();
        User admin = User.synchronize(
                ADMIN_ID,
                "학사일정 관리자",
                "academic-schedule-service-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        entityManager.persist(admin);
        entityManager.flush();
    }

    @Test
    void createNormalizesInputsAndRecordsAuditMetadata() {
        AcademicScheduleDetailResponseDTO created = academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(
                        "  수강신청 안내  ", "   ", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
                ),
                ADMIN,
                "academic-schedule-create-request",
                "127.0.0.1"
        );

        assertThat(created.title()).isEqualTo("수강신청 안내");
        assertThat(created.content()).isNull();
        assertThat(created.isActive()).isTrue();
        assertThat(created.createdAt()).isNotNull();

        AcademicSchedule saved = academicScheduleRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getAuthor().getId()).isEqualTo(ADMIN_ID);

        AuditLog auditLog = auditLogRepository.findAll().getFirst();
        assertThat(auditLog.getAction()).isEqualTo("ACADEMIC_SCHEDULE_CREATE");
        assertThat(auditLog.getTargetType()).isEqualTo("ACADEMIC_SCHEDULE");
        assertThat(auditLog.getTargetId()).isEqualTo(created.id());
        assertThat(auditLog.getAfterValue()).containsEntry("authorId", ADMIN_ID);
        assertThat(auditLog.getRequestId()).isEqualTo("academic-schedule-create-request");
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void createRejectsDuplicateActiveSchedule() {
        create("수강신청 안내", "같은 내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL);

        assertThatThrownBy(() -> create(
                "수강신청 안내", "같은 내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
        )).isInstanceOf(DuplicateAcademicScheduleException.class);
    }

    @Test
    void mutationRejectsOversizedTitleAndContent() {
        assertThatThrownBy(() -> academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(
                        "제".repeat(101), "본문", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
                ),
                ADMIN, null, null
        )).isInstanceOf(InvalidAcademicScheduleRequestException.class);

        assertThatThrownBy(() -> academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(
                        "제목", "가".repeat(5001), LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
                ),
                ADMIN, null, null
        )).isInstanceOf(InvalidAcademicScheduleRequestException.class);

        AcademicScheduleDetailResponseDTO schedule = create(
                "수정 대상", "본문", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
        );
        assertThatThrownBy(() -> academicScheduleService.update(
                schedule.id(),
                new AcademicScheduleUpdateRequestDTO(
                        "수정 대상", "가".repeat(5001), LocalDate.of(2026, 8, 17), null,
                        AcademicScheduleTargetRole.ALL, "본문 길이 초과 확인"
                ),
                ADMIN, null, null
        )).isInstanceOf(InvalidAcademicScheduleRequestException.class);
    }

    @Test
    void createAndUpdateRejectEndDateBeforeStartDate() {
        assertThatThrownBy(() -> academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(
                        "잘못된 기간", "본문", LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 21),
                        AcademicScheduleTargetRole.ALL
                ),
                ADMIN, null, null
        )).isInstanceOf(InvalidAcademicScheduleRequestException.class);

        AcademicScheduleDetailResponseDTO schedule = create(
                "기간 수정 대상", "본문", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
        );
        assertThatThrownBy(() -> academicScheduleService.update(
                schedule.id(),
                new AcademicScheduleUpdateRequestDTO(
                        "기간 수정 대상", "본문", LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 21),
                        AcademicScheduleTargetRole.ALL, "기간 오류 확인"
                ),
                ADMIN, null, null
        )).isInstanceOf(InvalidAcademicScheduleRequestException.class);
    }

    @Test
    void searchRestrictsGeneralUsersAndLetsAdminSeeAllStatuses() {
        create("전체 대상 일정", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL);
        AcademicScheduleDetailResponseDTO studentSchedule = create(
                "학생 대상 일정", "내용", LocalDate.of(2026, 8, 18), null, AcademicScheduleTargetRole.STUDENT
        );
        create("교수 대상 일정", "내용", LocalDate.of(2026, 8, 19), null, AcademicScheduleTargetRole.PROFESSOR);
        academicScheduleService.changeStatus(
                studentSchedule.id(), new AcademicScheduleStatusRequestDTO(false, "비활성 처리"), ADMIN, null, null
        );

        PageResponseDTO<AcademicScheduleSummaryResponseDTO> studentResult = academicScheduleService.search(
                search(null, null), STUDENT
        );
        assertThat(studentResult.items())
                .extracting(AcademicScheduleSummaryResponseDTO::title)
                .containsExactly("전체 대상 일정");

        PageResponseDTO<AcademicScheduleSummaryResponseDTO> professorResult = academicScheduleService.search(
                search(AcademicScheduleTargetRole.PROFESSOR, null), PROFESSOR
        );
        assertThat(professorResult.items())
                .extracting(AcademicScheduleSummaryResponseDTO::title)
                .containsExactly("교수 대상 일정");

        PageResponseDTO<AcademicScheduleSummaryResponseDTO> adminResult = academicScheduleService.search(
                search(null, null), ADMIN
        );
        assertThat(adminResult.totalCount()).isEqualTo(3);
        assertThat(adminResult.items()).extracting(AcademicScheduleSummaryResponseDTO::isActive)
                .containsExactly(true, false, true);
    }

    @Test
    void searchRejectsAnotherRoleFilterForGeneralUser() {
        assertThatThrownBy(() -> academicScheduleService.search(
                search(AcademicScheduleTargetRole.PROFESSOR, null), STUDENT
        )).isInstanceOf(AcademicScheduleAccessDeniedException.class);
    }

    @Test
    void detailAppliesRoleAndInactiveVisibilityRules() {
        AcademicScheduleDetailResponseDTO professorSchedule = create(
                "교수 일정", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.PROFESSOR
        );
        AcademicScheduleDetailResponseDTO studentSchedule = create(
                "학생 일정", "내용", LocalDate.of(2026, 8, 18), null, AcademicScheduleTargetRole.STUDENT
        );
        academicScheduleService.changeStatus(
                studentSchedule.id(), new AcademicScheduleStatusRequestDTO(false, "비활성 처리"), ADMIN, null, null
        );

        assertThatThrownBy(() -> academicScheduleService.get(professorSchedule.id(), STUDENT))
                .isInstanceOf(AcademicScheduleAccessDeniedException.class);
        assertThat(academicScheduleService.get(studentSchedule.id(), ADMIN).isActive()).isFalse();
    }

    @Test
    void updateReturnsSameValueWithoutAuditForNoOpChange() {
        AcademicScheduleDetailResponseDTO schedule = create(
                "동일 값 일정", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
        );
        int initialAuditCount = auditLogRepository.findAll().size();

        AcademicScheduleDetailResponseDTO same = academicScheduleService.update(
                schedule.id(),
                new AcademicScheduleUpdateRequestDTO(
                        "동일 값 일정", "내용", LocalDate.of(2026, 8, 17), null,
                        AcademicScheduleTargetRole.ALL, "변경 없음 확인"
                ),
                ADMIN, null, null
        );

        assertThat(same.id()).isEqualTo(schedule.id());
        assertThat(auditLogRepository.findAll()).hasSize(initialAuditCount);
    }

    @Test
    void updateRejectsResultMatchingAnotherActiveSchedule() {
        create("기존 일정", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL);
        AcademicScheduleDetailResponseDTO target = create(
                "변경 대상 일정", "내용", LocalDate.of(2026, 8, 18), null, AcademicScheduleTargetRole.STUDENT
        );

        assertThatThrownBy(() -> academicScheduleService.update(
                target.id(),
                new AcademicScheduleUpdateRequestDTO(
                        "기존 일정", "내용", LocalDate.of(2026, 8, 17), null,
                        AcademicScheduleTargetRole.ALL, "중복 확인"
                ),
                ADMIN, null, null
        )).isInstanceOf(DuplicateAcademicScheduleException.class);
    }

    @Test
    void changeStatusSkipsNoOpAndRejectsReactivationDuplicate() {
        AcademicScheduleDetailResponseDTO schedule = create(
                "상태 변경 대상", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
        );

        academicScheduleService.changeStatus(
                schedule.id(), new AcademicScheduleStatusRequestDTO(false, "일정 취소"), ADMIN, null, null
        );
        int auditCountAfterDeactivate = auditLogRepository.findAll().size();

        academicScheduleService.changeStatus(
                schedule.id(), new AcademicScheduleStatusRequestDTO(false, "재요청"), ADMIN, null, null
        );
        assertThat(auditLogRepository.findAll()).hasSize(auditCountAfterDeactivate);

        create("상태 변경 대상", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL);

        assertThatThrownBy(() -> academicScheduleService.changeStatus(
                schedule.id(), new AcademicScheduleStatusRequestDTO(true, "재개"), ADMIN, null, null
        )).isInstanceOf(DuplicateAcademicScheduleException.class);
    }

    @Test
    void mutationRejectsNonAdminEvenOutsideController() {
        assertThatThrownBy(() -> academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(
                        "학생 작성", "내용", LocalDate.of(2026, 8, 17), null, AcademicScheduleTargetRole.ALL
                ),
                STUDENT, null, null
        )).isInstanceOf(AcademicScheduleAccessDeniedException.class);
    }

    private AcademicScheduleDetailResponseDTO create(String title, String content, LocalDate startDate,
                                                      LocalDate endDate, AcademicScheduleTargetRole targetRole) {
        return academicScheduleService.create(
                new AcademicScheduleCreateRequestDTO(title, content, startDate, endDate, targetRole),
                ADMIN, null, null
        );
    }

    private AcademicScheduleSearchRequestDTO search(AcademicScheduleTargetRole targetRole, Boolean active) {
        return new AcademicScheduleSearchRequestDTO(1, 20, null, null, null, targetRole, active);
    }
}
