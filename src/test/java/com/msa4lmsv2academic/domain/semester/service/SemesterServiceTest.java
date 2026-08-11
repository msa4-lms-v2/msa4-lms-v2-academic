package com.msa4lmsv2academic.domain.semester.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.semester.request.SemesterCreateRequestDTO;
import com.msa4lmsv2academic.domain.semester.request.SemesterSearchRequestDTO;
import com.msa4lmsv2academic.domain.semester.response.SemesterResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.DuplicateSemesterException;
import com.msa4lmsv2academic.global.error.InvalidSemesterRequestException;
import com.msa4lmsv2academic.global.error.SemesterAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SemesterServiceTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9001L;
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_ID, "ADMIN");

    @Autowired
    private SemesterService semesterService;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        semesterRepository.deleteAllInBatch();
        entityManager.persist(User.synchronize(
                ADMIN_ID,
                "학사관리자",
                "semester-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        ));
        entityManager.flush();
    }

    @Test
    void createDefaultsToNotCurrentAndRecordsAuditMetadata() {
        SemesterResponseDTO created = semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, null),
                ADMIN,
                "request-2026-first",
                "127.0.0.1"
        );

        assertThat(created.isCurrent()).isFalse();
        assertThat(created.academicYear()).isEqualTo((short) 2026);

        AuditLog auditLog = auditLogRepository.findAll().getFirst();
        assertThat(auditLog.getActorId()).isEqualTo(ADMIN_ID);
        assertThat(auditLog.getAction()).isEqualTo("SEMESTER_CREATE");
        assertThat(auditLog.getTargetType()).isEqualTo("SEMESTER");
        assertThat(auditLog.getTargetId()).isEqualTo(created.id());
        assertThat(auditLog.getBeforeValue()).isNull();
        assertThat(auditLog.getAfterValue()).containsEntry("term", "FIRST");
        assertThat(auditLog.getAfterValue()).containsEntry("isCurrent", false);
        assertThat(auditLog.getRequestId()).isEqualTo("request-2026-first");
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(auditLog.getCreatedAt()).isNotNull();
    }

    @Test
    void createCurrentSemesterUnsetsPreviousCurrentAndAuditsBothChanges() {
        SemesterResponseDTO previous = semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, true), ADMIN, "request-1", "127.0.0.1"
        );

        SemesterResponseDTO current = semesterService.createSemester(
                request((short) 2026, SemesterTerm.SECOND, true), ADMIN, "request-2", "127.0.0.1"
        );

        Semester previousEntity = semesterRepository.findById(previous.id()).orElseThrow();
        Semester currentEntity = semesterRepository.findById(current.id()).orElseThrow();
        assertThat(previousEntity.isCurrent()).isFalse();
        assertThat(currentEntity.isCurrent()).isTrue();
        assertThat(semesterRepository.findCurrentSemestersForUpdate())
                .extracting(Semester::getId)
                .containsExactly(current.id());

        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).hasSize(3);
        AuditLog unsetLog = auditLogs.stream()
                .filter(log -> "SEMESTER_CURRENT_UNSET".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(unsetLog.getTargetId()).isEqualTo(previous.id());
        assertThat(unsetLog.getBeforeValue()).containsEntry("isCurrent", true);
        assertThat(unsetLog.getAfterValue()).containsEntry("isCurrent", false);
    }

    @Test
    void createRejectsDuplicateAcademicYearAndTerm() {
        semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, false), ADMIN, null, null
        );

        assertThatThrownBy(() -> semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, true), ADMIN, null, null
        )).isInstanceOf(DuplicateSemesterException.class);
    }

    @Test
    void createRejectsInvalidPeriodOrder() {
        SemesterCreateRequestDTO invalid = new SemesterCreateRequestDTO(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 6, 19),
                LocalDate.of(2026, 3, 2),
                LocalDateTime.of(2026, 2, 20, 18, 0),
                LocalDateTime.of(2026, 2, 16, 9, 0),
                false
        );

        assertThatThrownBy(() -> semesterService.createSemester(invalid, ADMIN, null, null))
                .isInstanceOf(InvalidSemesterRequestException.class);
    }

    @Test
    void createRejectsNonAdminEvenWhenCalledOutsideController() {
        assertThatThrownBy(() -> semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, false),
                new CurrentUser(100L, "PROFESSOR"),
                null,
                null
        )).isInstanceOf(SemesterAccessDeniedException.class);
    }

    @Test
    void searchFiltersAndSortsSemestersWithPageMetadata() {
        semesterService.createSemester(
                request((short) 2025, SemesterTerm.SECOND, false), ADMIN, null, null
        );
        semesterService.createSemester(
                request((short) 2026, SemesterTerm.FIRST, false), ADMIN, null, null
        );
        semesterService.createSemester(
                request((short) 2026, SemesterTerm.SECOND, true), ADMIN, null, null
        );

        PageRes<SemesterResponseDTO> result = semesterService.searchSemesters(
                new SemesterSearchRequestDTO(1, 1, (short) 2026, null, null)
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().term()).isEqualTo(SemesterTerm.SECOND);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.hasNext()).isTrue();

        PageRes<SemesterResponseDTO> currentOnly = semesterService.searchSemesters(
                new SemesterSearchRequestDTO(1, 20, null, null, true)
        );
        assertThat(currentOnly.items())
                .extracting(SemesterResponseDTO::term)
                .containsExactly(SemesterTerm.SECOND);
    }

    private SemesterCreateRequestDTO request(short year, SemesterTerm term, Boolean current) {
        int startMonth = term == SemesterTerm.FIRST ? 3 : 9;
        int endMonth = term == SemesterTerm.FIRST ? 6 : 12;
        int enrollmentMonth = term == SemesterTerm.FIRST ? 2 : 8;
        return new SemesterCreateRequestDTO(
                year,
                term,
                LocalDate.of(year, startMonth, 2),
                LocalDate.of(year, endMonth, 18),
                LocalDateTime.of(year, enrollmentMonth, 16, 9, 0),
                LocalDateTime.of(year, enrollmentMonth, 20, 18, 0),
                current
        );
    }
}
