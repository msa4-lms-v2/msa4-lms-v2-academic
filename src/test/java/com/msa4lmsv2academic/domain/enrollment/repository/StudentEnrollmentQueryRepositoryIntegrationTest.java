package com.msa4lmsv2academic.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
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
class StudentEnrollmentQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 92001L;
    private static final long STUDENT_ID = 92001L;
    private static final long PROFESSOR_ID = 92001L;

    @Autowired
    private StudentEnrollmentQueryRepository studentEnrollmentQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (92001, 'CLASS-COL', '강의조회대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (92001, '222', 92001, '강의조회학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '조회학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (92002, '조회교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, 92002, 2020, 92001)", PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 92001, 3, 2024, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);

        insertSemester(92001L, "FIRST");
        insertSemester(92002L, "SECOND");
        insertCourse(92001L, "CLASS-01", "자료구조");
        insertCourse(92002L, "CLASS-02", "운영체제");
        insertCourse(92003L, "CLASS-03", "취소강의");
        insertLecture(92001L, 92001L, 92001L, "01");
        insertLecture(92002L, 92002L, 92002L, "01");
        insertLecture(92003L, 92003L, 92001L, "02");
        insertEnrollment(92001L, 92001L, "ACTIVE");
        insertEnrollment(92002L, 92002L, "ACTIVE");
        insertEnrollment(92003L, 92003L, "CANCELLED");
    }

    @Test
    void returnsOnlyActiveEnrollmentsMatchingAcademicYearAndTerm() {
        List<StudentEnrollmentQueryResult> result =
                studentEnrollmentQueryRepository.findActiveEnrollmentsByStudentUserId(
                        STUDENT_USER_ID,
                        (short) 2026,
                        SemesterTerm.FIRST
                );

        assertThat(result).extracting(StudentEnrollmentQueryResult::courseName).containsExactly("자료구조");
        assertThat(result.getFirst().enrollmentId()).isEqualTo(92001L);
        assertThat(result.getFirst().enrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(result.getFirst().enrolledAt()).isEqualTo(LocalDateTime.of(2026, 2, 2, 9, 0));
        assertThat(result.getFirst().departmentName()).isEqualTo("강의조회학과");
        assertThat(result.getFirst().professorName()).isEqualTo("조회교수");
    }

    @Test
    void returnsEmptyListWhenStudentHasNoMatchingEnrollments() {
        List<StudentEnrollmentQueryResult> result =
                studentEnrollmentQueryRepository.findActiveEnrollmentsByStudentUserId(
                        STUDENT_USER_ID,
                        (short) 2025,
                        SemesterTerm.FIRST
                );

        assertThat(result).isEmpty();
    }

    @Test
    void verifiesAcademicStudentProfileByAuthenticatedUserId() {
        assertThat(studentEnrollmentQueryRepository.existsStudentByUserId(STUDENT_USER_ID)).isTrue();
        assertThat(studentEnrollmentQueryRepository.existsStudentByUserId(99999L)).isFalse();
    }

    private void insertSemester(long id, String term) {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (?, 2026, ?, '2026-03-02', '2026-06-19', "
                + "'2026-02-01 09:00:00', '2026-02-07 18:00:00', 0)", id, term);
    }

    private void insertCourse(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 92001, ?, ?, 3, 3, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(long id, long courseId, long semesterId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, ?, 40, '공학관 301호', 'OPEN', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId, PROFESSOR_ID, sectionNo);
    }

    private void insertEnrollment(long id, long lectureId, String status) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, ?, ?, ?, ?, 'DRAFT')",
                id, STUDENT_ID, lectureId, status, LocalDateTime.of(2026, 2, 2, 9, 0));
    }
}
