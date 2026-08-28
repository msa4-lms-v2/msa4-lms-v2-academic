package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.error.EnrollmentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudentEnrollmentCancellationIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 92901L;
    private static final long STUDENT_ID = 92901L;
    private static final long PROFESSOR_ID = 92901L;
    private static final long SEMESTER_ID = 92901L;
    private static final long LECTURE_ID = 92901L;
    private static final long ENROLLMENT_ID = 92901L;

    @Autowired
    private StudentEnrollmentCancellationService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (92901, 'CANCEL-COL', '수강취소대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (92901, '291', 92901, '수강취소학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '취소학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (92902, '취소교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, 92902, 2020, 92901)", PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 92901, 3, 2024, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (?, 2026, 'FIRST', '2026-03-02', '2026-06-19', "
                + "NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY, 0)", SEMESTER_ID);
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (92901, 92901, 'CANCEL-01', '수강취소테스트', 3, 3, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, 92901, ?, '01', 40, '공학관 301호', 'OPEN', 30, 30, 30, 10, NULL)",
                LECTURE_ID, SEMESTER_ID, PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, ?, ?, 'ACTIVE', NOW(), 'DRAFT')",
                ENROLLMENT_ID, STUDENT_ID, LECTURE_ID);
    }

    @Test
    void changesStatusAndWritesCancelHistoryAtomically() {
        var response = service.cancel(ENROLLMENT_ID, new CurrentUser(STUDENT_USER_ID, "STUDENT"));

        assertThat(response.enrollmentId()).isEqualTo(ENROLLMENT_ID);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM enrollments WHERE id = ?", String.class, ENROLLMENT_ID
        )).isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollment_histories "
                        + "WHERE student_id = ? AND lecture_id = ? AND action = 'CANCEL'",
                Long.class, STUDENT_ID, LECTURE_ID
        )).isEqualTo(1L);
    }

    @Test
    void rejectsRepeatedCancellationWithoutAdditionalHistory() {
        CurrentUser currentUser = new CurrentUser(STUDENT_USER_ID, "STUDENT");
        service.cancel(ENROLLMENT_ID, currentUser);

        assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, currentUser))
                .isInstanceOf(EnrollmentApplicationRejectedException.class)
                .hasMessageContaining("이미 취소");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollment_histories WHERE action = 'CANCEL' AND lecture_id = ?",
                Long.class, LECTURE_ID
        )).isEqualTo(1L);
    }

    @Test
    void rejectsCancellationOutsidePeriodWithoutPartialUpdate() {
        jdbcTemplate.update("UPDATE semesters SET enrollment_start_at = '2000-01-01', "
                + "enrollment_end_at = '2000-01-02' WHERE id = ?", SEMESTER_ID);

        assertThatThrownBy(() -> service.cancel(
                ENROLLMENT_ID,
                new CurrentUser(STUDENT_USER_ID, "STUDENT")
        )).isInstanceOf(EnrollmentApplicationRejectedException.class)
                .hasMessageContaining("수강신청 기간");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM enrollments WHERE id = ?", String.class, ENROLLMENT_ID
        )).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enrollment_histories WHERE action = 'CANCEL' AND lecture_id = ?",
                Long.class, LECTURE_ID
        )).isZero();
    }

    @Test
    void doesNotExposeAnotherStudentsEnrollment() {
        assertThatThrownBy(() -> service.cancel(
                ENROLLMENT_ID,
                new CurrentUser(99999L, "STUDENT")
        )).isInstanceOf(EnrollmentNotFoundException.class);
    }
}
