package com.msa4lmsv2academic.domain.grade.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudentGradeQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 98401L;
    private static final long STUDENT_ID = 98401L;

    @Autowired
    private StudentGradeQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (98401, 'GR-COL', '성적조회대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (98401, '984', 98401, '성적조회학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '성적조회학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (98402, '다른학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (98403, '성적조회교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (98401, 0, 98403, 2020, 98401)");
        insertStudent(STUDENT_ID, STUDENT_USER_ID);
        insertStudent(98402L, 98402L);
        insertSemester(98401L, 2025, "SECOND");
        insertSemester(98402L, 2026, "FIRST");
        insertCourse(98401L, "GRADE-01", "자료구조");
        insertCourse(98402L, "GRADE-02", "운영체제");
        insertLecture(98401L, 98401L, 98401L);
        insertLecture(98402L, 98401L, 98402L);
        insertLecture(98403L, 98402L, 98402L);
        insertEnrollment(98401L, STUDENT_ID, 98401L, "ACTIVE", "OPENED", "C");
        insertEnrollment(98402L, STUDENT_ID, 98402L, "ACTIVE", "OPENED", "A");
        insertEnrollment(98403L, STUDENT_ID, 98403L, "ACTIVE", "DRAFT", null);
        insertEnrollment(98404L, STUDENT_ID, 98403L, "CANCELLED", "OPENED", "B");
        insertEnrollment(98405L, 98402L, 98403L, "ACTIVE", "OPENED", "A+");
    }

    @Test
    void returnsOnlyAuthenticatedStudentsActiveOpenedGrades() {
        List<StudentGradeQueryResult> result = repository.findOpenedGradesByStudentUserId(STUDENT_USER_ID);

        assertThat(result).extracting(StudentGradeQueryResult::enrollmentId)
                .containsExactly(98402L, 98401L);
        assertThat(result).extracting(StudentGradeQueryResult::letterGrade)
                .containsExactly("A", "C");
    }

    @Test
    void checksStudentExistenceUsingAuthenticatedUserId() {
        assertThat(repository.existsStudentByUserId(STUDENT_USER_ID)).isTrue();
        assertThat(repository.existsStudentByUserId(99999L)).isFalse();
    }

    private void insertStudent(long id, long userId) {
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 98401, 3, 2024, 'ENROLLED', 98401)",
                id, userId);
    }

    private void insertSemester(long id, int academicYear, String term) {
        jdbcTemplate.update("INSERT INTO semesters "
                        + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                        + "VALUES (?, ?, ?, '2026-03-02', '2026-06-19', "
                        + "'2026-02-01 09:00:00', '2026-02-07 18:00:00', 0)",
                id, academicYear, term);
    }

    private void insertCourse(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 98401, ?, ?, 3, 3, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(long id, long courseId, long semesterId) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, 98401, '01', 40, '공학관 301호', 'OPEN', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId);
    }

    private void insertEnrollment(
            long id,
            long studentId,
            long lectureId,
            String status,
            String gradeStatus,
            String letterGrade
    ) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, total_score, letter_grade, grade_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                studentId,
                lectureId,
                status,
                LocalDateTime.of(2026, 2, 2, 9, 0),
                letterGrade == null ? null : new BigDecimal("90.00"),
                letterGrade,
                gradeStatus);
    }
}
