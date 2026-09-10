package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCourseAttemptLimitRejectionReason;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.global.error.EnrollmentCourseAttemptLimitNotAllowedException;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EnrollmentCourseRuleValidatorIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_ID = 99001L;
    private static final long SEMESTER_ID = 99001L;
    private static final long TARGET_LECTURE_ID = 99001L;

    @Autowired private EnrollmentCourseRuleValidator validator;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (99001, 'VLD-COL', '검증대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) VALUES (99001, '990', 99001, '검증학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (99011, '검증학생', 'STUDENT', 'ACTIVE'), (99013, '검증교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) VALUES (99001, 0, 99013, 2020, 99001)");
        jdbcTemplate.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) VALUES (99001, 99011, 99001, 3, 2088, 'ENROLLED', 99001)");
        jdbcTemplate.update("INSERT INTO semesters (id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) VALUES (99001, 2090, 'SECOND', '2090-03-02', '2090-06-19', '2090-02-01 09:00:00', '2090-02-07 18:00:00', 0)");
        jdbcTemplate.update("INSERT INTO enrollment_credit_limit_rules (id, semester_id, max_credits, is_active) VALUES (99001, 99001, 9, 1)");
        jdbcTemplate.update("INSERT INTO courses (id, department_id, code, name, credits, target_grade, completion_type) VALUES (99001, 99001, 'VLD-99001', '검증교과목', 3, 3, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures (id, semester_id, course_id, professor_id, section_no, capacity, status, midterm_ratio, final_ratio, assignment_ratio, attendance_ratio) VALUES (99001, 99001, 99001, 99001, '01', 40, 'OPEN', 30, 30, 30, 10)");
    }

    @Test
    void allowsSecondAttemptWhenPreviousGradeIsWithinRetakeRange() {
        insertEnrollment(99001L, "C+");

        assertThatCode(() -> validator.validate(STUDENT_ID, targetLecture())).doesNotThrowAnyException();
    }

    @Test
    void rejectsThirdNonCancelledAttempt() {
        insertEnrollment(99001L, "C");
        insertEnrollment(99002L, "D");

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, targetLecture()))
                .isInstanceOfSatisfying(EnrollmentCourseAttemptLimitNotAllowedException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getReason())
                                .isEqualTo(EnrollmentCourseAttemptLimitRejectionReason.COURSE_ATTEMPT_LIMIT_EXCEEDED));
    }

    @Test
    void ignoresCancelledAttemptWhenCountingCourseAttempts() {
        jdbcTemplate.update("INSERT INTO enrollments (id, student_id, lecture_id, status, enrolled_at, grade_status) VALUES (99001, 99001, 99001, 'CANCELLED', '2090-02-02 09:00:00', 'DRAFT')");
        insertEnrollment(99002L, "D+");

        assertThatCode(() -> validator.validate(STUDENT_ID, targetLecture())).doesNotThrowAnyException();
    }

    private Lecture targetLecture() {
        return entityManager.find(Lecture.class, TARGET_LECTURE_ID);
    }

    private void insertEnrollment(long id, String grade) {
        jdbcTemplate.update("INSERT INTO enrollments (id, student_id, lecture_id, status, enrolled_at, grade_status, letter_grade) VALUES (?, 99001, 99001, 'ACTIVE', '2090-02-02 09:00:00', 'OPENED', ?)", id, grade);
    }
}
