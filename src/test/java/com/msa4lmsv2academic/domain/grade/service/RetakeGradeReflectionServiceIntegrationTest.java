package com.msa4lmsv2academic.domain.grade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.grade.request.RetakeGradeReflectionRequestDTO;
import com.msa4lmsv2academic.global.error.RetakeGradeReflectionConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RetakeGradeReflectionServiceIntegrationTest extends MySqlIntegrationTest {

    private static final long ADMIN_USER_ID = 94001L;
    private static final long STUDENT_USER_ID = 94002L;
    private static final long PROFESSOR_USER_ID = 94003L;
    private static final long STUDENT_ID = 94001L;
    private static final long PROFESSOR_ID = 94001L;
    private static final long PREVIOUS_ENROLLMENT_ID = 94001L;
    private static final long RETAKE_ENROLLMENT_ID = 94003L;

    @Autowired
    private RetakeGradeReflectionService reflectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (94001, 'RG-COL', '재수강대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (94001, '241', 94001, '재수강학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '재수강관리자', 'ADMIN', 'ACTIVE')",
                ADMIN_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '재수강학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '재수강교수', 'PROFESSOR', 'ACTIVE')",
                PROFESSOR_USER_ID);
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, 94001)", PROFESSOR_ID, PROFESSOR_USER_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 94001, 2, 2025, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);
        insertSemester(94001L, 2025, "SECOND", false);
        insertSemester(94002L, 2026, "FIRST", true);
        insertCourse(94001L, "RG-CORE", "재수강과목", 3);
        insertCourse(94002L, "RG-OTHER", "다른과목", 3);
        insertLecture(94001L, 94001L, 94001L, "01");
        insertLecture(94002L, 94001L, 94002L, "01");
        insertLecture(94003L, 94002L, 94001L, "01");
        insertEnrollment(PREVIOUS_ENROLLMENT_ID, 94001L, "C");
        insertEnrollment(94002L, 94002L, "B");
        insertEnrollment(RETAKE_ENROLLMENT_ID, 94003L, "A");
    }

    @Test
    void reflectsLatestRetakeAndRecalculatesSummariesWithHistory() {
        var response = reflectionService.reflect(
                RETAKE_ENROLLMENT_ID,
                new RetakeGradeReflectionRequestDTO("재수강 확정 성적 반영"),
                new CurrentUser(ADMIN_USER_ID, "ADMIN")
        );

        assertThat(response.previousEnrollmentId()).isEqualTo(PREVIOUS_ENROLLMENT_ID);
        assertThat(response.previousGrade()).isEqualTo("C");
        assertThat(response.reflectedGrade()).isEqualTo("A");
        assertThat(response.processedBy()).isEqualTo(ADMIN_USER_ID);
        assertThat(response.summaries()).hasSize(2);
        assertThat(response.summaries()).anySatisfy(summary -> {
            assertThat(summary.semesterId()).isEqualTo(94001L);
            assertThat(summary.totalCredits()).isEqualTo((short) 3);
            assertThat(summary.gpa()).isEqualByComparingTo(new BigDecimal("3.00"));
        });
        assertThat(response.summaries()).anySatisfy(summary -> {
            assertThat(summary.semesterId()).isEqualTo(94002L);
            assertThat(summary.totalCredits()).isEqualTo((short) 3);
            assertThat(summary.gpa()).isEqualByComparingTo(new BigDecimal("4.00"));
        });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_grade_summaries WHERE student_id = ?", Integer.class, STUDENT_ID))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT previous_value FROM grade_correction_histories WHERE enrollment_id = ?",
                String.class, RETAKE_ENROLLMENT_ID)).isEqualTo(PREVIOUS_ENROLLMENT_ID + ":C");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT new_value FROM grade_correction_histories WHERE enrollment_id = ?",
                String.class, RETAKE_ENROLLMENT_ID)).isEqualTo(RETAKE_ENROLLMENT_ID + ":A");
    }

    @Test
    void rejectsDuplicateReflection() {
        var request = new RetakeGradeReflectionRequestDTO("재수강 확정 성적 반영");
        var administrator = new CurrentUser(ADMIN_USER_ID, "ADMIN");
        reflectionService.reflect(RETAKE_ENROLLMENT_ID, request, administrator);

        assertThatThrownBy(() -> reflectionService.reflect(RETAKE_ENROLLMENT_ID, request, administrator))
                .isInstanceOf(RetakeGradeReflectionConflictException.class)
                .hasMessageContaining("이미 반영");
    }

    @Test
    void rejectsRetakeReflectionWhenPreviousHighGradeExists() {
        jdbcTemplate.update("UPDATE enrollments SET letter_grade = 'B' WHERE id = ?", PREVIOUS_ENROLLMENT_ID);

        assertThatThrownBy(() -> reflectionService.reflect(
                RETAKE_ENROLLMENT_ID,
                new RetakeGradeReflectionRequestDTO("잘못 수강된 재수강 반영 시도"),
                new CurrentUser(ADMIN_USER_ID, "ADMIN")
        )).isInstanceOf(RetakeGradeReflectionConflictException.class)
                .hasMessageContaining("B 이상");
    }

    @Test
    void doesNotCreateSummaryForSemesterWithOnlyDraftGrades() {
        insertSemester(94003L, 2026, "SECOND", false);
        insertCourse(94003L, "RG-DRAFT", "성적미공개과목", 2);
        insertLecture(94004L, 94003L, 94003L, "01");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, letter_grade, grade_status) "
                        + "VALUES (94004, ?, 94004, 'ACTIVE', ?, NULL, 'DRAFT')",
                STUDENT_ID, LocalDateTime.of(2026, 8, 3, 9, 0));

        var response = reflectionService.reflect(
                RETAKE_ENROLLMENT_ID,
                new RetakeGradeReflectionRequestDTO("재수강 확정 성적 반영"),
                new CurrentUser(ADMIN_USER_ID, "ADMIN")
        );

        assertThat(response.summaries())
                .noneMatch(summary -> summary.semesterId().equals(94003L));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM student_grade_summaries WHERE student_id = ? AND semester_id = ?",
                Integer.class, STUDENT_ID, 94003L)).isZero();
    }

    private void insertSemester(long id, int year, String term, boolean current) {
        jdbcTemplate.update("INSERT INTO semesters "
                        + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                        + "VALUES (?, ?, ?, '2026-03-02', '2026-06-19', "
                        + "'2026-02-01 09:00:00', '2026-02-07 18:00:00', ?)",
                id, year, term, current);
    }

    private void insertCourse(long id, String code, String name, int credits) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 94001, ?, ?, ?, NULL, 'MAJOR_REQUIRED')",
                id, code, name, credits);
    }

    private void insertLecture(long id, long semesterId, long courseId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, ?, 40, 'A101', 'CLOSED', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId, PROFESSOR_ID, sectionNo);
    }

    private void insertEnrollment(long id, long lectureId, String letterGrade) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, letter_grade, grade_status) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, ?, 'OPENED')",
                id, STUDENT_ID, lectureId, LocalDateTime.of(2026, 2, 2, 9, 0), letterGrade);
    }
}
