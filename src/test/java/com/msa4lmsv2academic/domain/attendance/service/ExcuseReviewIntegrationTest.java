package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseReviewRequestDTO;
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
class ExcuseReviewIntegrationTest extends MySqlIntegrationTest {

    private static final long REQUEST_ID = 99201L;
    private static final long PROFESSOR_USER_ID = 99202L;

    @Autowired
    private ExcuseReviewService excuseReviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99201, 'REVIEW-COL', '공결심사테스트대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99201, 'REV', 99201, '공결심사테스트학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99202, '공결심사교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (99203, '공결심사학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (99201, 0, 99202, 2020, 99201)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (99201, 99203, 99201, 2, 2025, 'ENROLLED', 99201)");
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99201, 2026, 'SECOND', '2026-08-31', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (99201, 99201, 'REVIEW-01', '공결심사테스트강의', 3, 2, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                + "VALUES (99201, 99201, 99201, 99201, '01', 40, '공학관 302호', 'OPEN', "
                + "30, 30, 30, 10, '공결심사 테스트 강의계획서')");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (99201, 99201, 99201, 'ACTIVE', ?, 'DRAFT')",
                LocalDateTime.of(2026, 8, 5, 9, 0));
        jdbcTemplate.update("INSERT INTO excuse_requests "
                + "(id, enrollment_id, lecture_date, period, reason, status) "
                + "VALUES (99201, 99201, '2026-09-01', 2, '병원 진료', 'PENDING')");
    }

    @Test
    void approvesOnceAndReplaysSameSuccessfulRequest() {
        ExcuseReviewRequestDTO request = new ExcuseReviewRequestDTO(ExcuseRequestStatus.APPROVED, null);
        CurrentUser professor = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");

        var first = excuseReviewService.review(
                REQUEST_ID,
                request,
                "excuse-review-integration-approved",
                professor,
                "trace-approved",
                "127.0.0.1"
        );
        var replay = excuseReviewService.review(
                REQUEST_ID,
                request,
                "excuse-review-integration-approved",
                professor,
                "trace-retry",
                "127.0.0.1"
        );

        assertThat(first.data().status()).isEqualTo(ExcuseRequestStatus.APPROVED);
        assertThat(replay).isEqualTo(first);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM excuse_requests WHERE id = ?", String.class, REQUEST_ID
        )).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'EXCUSE_REQUEST' "
                        + "AND target_id = ? AND action = 'EXCUSE_APPROVED'",
                Long.class,
                REQUEST_ID
        )).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(before_value, '$.status')) FROM audit_logs "
                        + "WHERE target_type = 'EXCUSE_REQUEST' AND target_id = ?",
                String.class,
                REQUEST_ID
        )).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(after_value, '$.status')) FROM audit_logs "
                        + "WHERE target_type = 'EXCUSE_REQUEST' AND target_id = ?",
                String.class,
                REQUEST_ID
        )).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM idempotency_keys WHERE idempotency_key = ?",
                String.class,
                "excuse-review-integration-approved"
        )).isEqualTo("COMPLETED");
    }

    @Test
    void rejectsWithReasonAndRecordsProcessor() {
        var response = excuseReviewService.review(
                REQUEST_ID,
                new ExcuseReviewRequestDTO(ExcuseRequestStatus.REJECTED, "  증빙 불충분  "),
                "excuse-review-integration-rejected",
                new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR"),
                "trace-rejected",
                "127.0.0.1"
        );

        assertThat(response.data().status()).isEqualTo(ExcuseRequestStatus.REJECTED);
        assertThat(response.data().rejectReason()).isEqualTo("증빙 불충분");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reject_reason FROM excuse_requests WHERE id = ?", String.class, REQUEST_ID
        )).isEqualTo("증빙 불충분");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT actor_id FROM audit_logs WHERE target_type = 'EXCUSE_REQUEST' AND target_id = ?",
                Long.class,
                REQUEST_ID
        )).isEqualTo(PROFESSOR_USER_ID);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reason FROM audit_logs WHERE target_type = 'EXCUSE_REQUEST' AND target_id = ?",
                String.class,
                REQUEST_ID
        )).isEqualTo("증빙 불충분");
    }
}
