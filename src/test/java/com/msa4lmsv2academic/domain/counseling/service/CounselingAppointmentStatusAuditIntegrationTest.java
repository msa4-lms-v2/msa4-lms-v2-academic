package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentStatusRequestDTO;
import com.msa4lmsv2academic.global.error.CounselingStatusConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CounselingAppointmentStatusAuditIntegrationTest extends MySqlIntegrationTest {

    private static final long COLLEGE_ID = 94001L;
    private static final long DEPARTMENT_ID = 94001L;
    private static final long PROFESSOR_USER_ID = 94001L;
    private static final long STUDENT_USER_ID = 94002L;
    private static final long PROFESSOR_ID = 94001L;
    private static final long STUDENT_ID = 94001L;
    private static final long APPOINTMENT_ID = 94001L;

    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser STUDENT = new CurrentUser(STUDENT_USER_ID, "STUDENT");

    @Autowired
    private CounselingAppointmentService counselingAppointmentService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "INSERT INTO colleges (id, code, name, active) VALUES (?, 'COUNSEL-AUDIT-COL', '상담감사대학', 1)",
                COLLEGE_ID
        );
        jdbcTemplate.update(
                "INSERT INTO departments (id, code, college_id, name, active) "
                        + "VALUES (?, '223', ?, '상담감사학과', 1)",
                DEPARTMENT_ID,
                COLLEGE_ID
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, role, status) VALUES (?, '상담 담당 교수', 'PROFESSOR', 'ACTIVE')",
                PROFESSOR_USER_ID
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, name, role, status) VALUES (?, '상담 신청 학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID
        );
        jdbcTemplate.update(
                "INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                        + "VALUES (?, 0, ?, 2020, ?)",
                PROFESSOR_ID,
                PROFESSOR_USER_ID,
                DEPARTMENT_ID
        );
        jdbcTemplate.update(
                "INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, ?, 3, 2024, 'ENROLLED', ?)",
                STUDENT_ID,
                STUDENT_USER_ID,
                DEPARTMENT_ID,
                PROFESSOR_ID
        );
        jdbcTemplate.update(
                "INSERT INTO counseling_appointments "
                        + "(id, student_id, professor_id, appointment_at, topic, status) "
                        + "VALUES (?, ?, ?, ?, '수강 계획 상담', 'PENDING')",
                APPOINTMENT_ID,
                STUDENT_ID,
                PROFESSOR_ID,
                LocalDateTime.now().plusDays(7).withSecond(0).withNano(0)
        );
    }

    @Test
    void professorRejectsPendingAppointmentAndAuditTracksActorReasonAndValues() {
        var response = counselingAppointmentService.changeStatus(
                APPOINTMENT_ID,
                new CounselingAppointmentStatusRequestDTO(
                        CounselingAppointmentStatus.REJECTED,
                        "신청 시간에는 온라인 상담이 어렵습니다."
                ),
                PROFESSOR
        );

        assertThat(response.status()).isEqualTo(CounselingAppointmentStatus.REJECTED);
        assertThat(response.professorNote()).isEqualTo("신청 시간에는 온라인 상담이 어렵습니다.");

        AuditLog audit = statusAudit("COUNSELING_APPOINTMENT_REJECTED");
        assertThat(audit.getActorId()).isEqualTo(PROFESSOR_USER_ID);
        assertThat(audit.getReason()).isEqualTo("신청 시간에는 온라인 상담이 어렵습니다.");
        assertThat(audit.getBeforeValue()).containsEntry("status", "PENDING");
        assertThat(audit.getAfterValue()).containsEntry("status", "REJECTED");
        assertThat(audit.getCreatedAt()).isNotNull();
        assertNotification(
                STUDENT_USER_ID,
                "APPOINTMENT_REJECTED",
                "PENDING",
                "REJECTED"
        );
    }

    @Test
    void studentCancelsOwnPendingAppointmentOnceAndAuditTracksTransition() {
        var response = counselingAppointmentService.changeStatus(
                APPOINTMENT_ID,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CANCELLED, null),
                STUDENT
        );

        assertThat(response.status()).isEqualTo(CounselingAppointmentStatus.CANCELLED);
        AuditLog audit = statusAudit("COUNSELING_APPOINTMENT_CANCELLED");
        assertThat(audit.getActorId()).isEqualTo(STUDENT_USER_ID);
        assertThat(audit.getBeforeValue()).containsEntry("status", "PENDING");
        assertThat(audit.getAfterValue()).containsEntry("status", "CANCELLED");
        assertNotification(
                PROFESSOR_USER_ID,
                "APPOINTMENT_CANCELLED",
                "PENDING",
                "CANCELLED"
        );

        assertThatThrownBy(() -> counselingAppointmentService.changeStatus(
                APPOINTMENT_ID,
                new CounselingAppointmentStatusRequestDTO(CounselingAppointmentStatus.CANCELLED, null),
                STUDENT
        )).isInstanceOf(CounselingStatusConflictException.class);
    }

    private AuditLog statusAudit(String action) {
        return auditLogRepository.findAll().stream()
                .filter(audit -> action.equals(audit.getAction()))
                .filter(audit -> "COUNSELING_APPOINTMENT".equals(audit.getTargetType()))
                .filter(audit -> Long.valueOf(APPOINTMENT_ID).equals(audit.getTargetId()))
                .findFirst()
                .orElseThrow();
    }

    private void assertNotification(
            long recipientUserId,
            String notificationType,
            String previousStatus,
            String newStatus
    ) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM counseling_notifications "
                        + "WHERE appointment_id = ? AND recipient_user_id = ? "
                        + "AND notification_type = ? AND previous_status = ? AND new_status = ?",
                Integer.class,
                APPOINTMENT_ID,
                recipientUserId,
                notificationType,
                previousStatus,
                newStatus
        );
        assertThat(count).isEqualTo(1);
    }
}
