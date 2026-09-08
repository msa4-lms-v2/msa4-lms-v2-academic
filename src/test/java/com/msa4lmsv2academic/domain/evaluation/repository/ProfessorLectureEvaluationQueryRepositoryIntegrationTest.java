package com.msa4lmsv2academic.domain.evaluation.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
class ProfessorLectureEvaluationQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long PROFESSOR_USER_ID = 98601L;
    private static final long PROFESSOR_ID = 98601L;
    private static final long OWNED_LECTURE_ID = 98601L;
    private static final long OTHER_LECTURE_ID = 98603L;

    @Autowired
    private ProfessorLectureEvaluationQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (98601, 'EVAL-COL', '강의평가대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (98601, 'EVL', 98601, '강의평가학과', 1)");
        insertUser(PROFESSOR_USER_ID, "담당교수", "PROFESSOR");
        insertUser(98602L, "다른교수", "PROFESSOR");
        insertUser(98603L, "평가학생1", "STUDENT");
        insertUser(98604L, "평가학생2", "STUDENT");
        insertUser(98605L, "취소학생", "STUDENT");
        insertProfessor(PROFESSOR_ID, PROFESSOR_USER_ID);
        insertProfessor(98602L, 98602L);
        insertStudent(98601L, 98603L);
        insertStudent(98602L, 98604L);
        insertStudent(98603L, 98605L);
        insertSemester(98601L, 2026, "FIRST", true);
        insertSemester(98602L, 2025, "SECOND", false);
        insertCourse(98601L, "EVAL-01", "소프트웨어공학");
        insertCourse(98602L, "EVAL-02", "자료구조");
        insertCourse(98603L, "EVAL-03", "다른교수강의");
        insertLecture(OWNED_LECTURE_ID, 98601L, 98601L, PROFESSOR_ID);
        insertLecture(98602L, 98602L, 98602L, PROFESSOR_ID);
        insertLecture(OTHER_LECTURE_ID, 98603L, 98601L, 98602L);
        insertEnrollment(98601L, 98601L, OWNED_LECTURE_ID, "ACTIVE");
        insertEnrollment(98602L, 98602L, OWNED_LECTURE_ID, "ACTIVE");
        insertEnrollment(98603L, 98603L, OWNED_LECTURE_ID, "CANCELLED");
        insertEnrollment(98604L, 98601L, OTHER_LECTURE_ID, "ACTIVE");
        insertEvaluation(98601L, 98601L, "{\"CONTENT_QUALITY\":5}", "좋았습니다.");
        insertEvaluation(98602L, 98602L, "{\"CONTENT_QUALITY\":3}", null);
        insertEvaluation(98603L, 98603L, "{\"CONTENT_QUALITY\":1}", "취소 응답");
        insertEvaluation(98604L, 98604L, "{\"CONTENT_QUALITY\":1}", "다른 교수 응답");
    }

    @Test
    void searchesOnlyOwnedLecturesWithFiltersAndActiveEnrollmentCount() {
        ProfessorLectureEvaluationSearchResult result = repository.searchLecturesByProfessorUserId(
                PROFESSOR_USER_ID,
                OWNED_LECTURE_ID,
                (short) 2026,
                SemesterTerm.FIRST,
                true,
                0L,
                20
        );

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
        var lecture = result.items().getFirst();
        assertThat(lecture.lectureId()).isEqualTo(OWNED_LECTURE_ID);
        assertThat(lecture.courseName()).isEqualTo("소프트웨어공학");
        assertThat(lecture.activeEnrollmentCount()).isEqualTo(2L);
    }

    @Test
    void excludesAnotherProfessorLectureAndSupportsEmptyAndPagedResults() {
        ProfessorLectureEvaluationSearchResult page = repository.searchLecturesByProfessorUserId(
                PROFESSOR_USER_ID, null, null, null, null, 0L, 1
        );
        ProfessorLectureEvaluationSearchResult hidden = repository.searchLecturesByProfessorUserId(
                PROFESSOR_USER_ID, OTHER_LECTURE_ID, null, null, null, 0L, 20
        );

        assertThat(page.totalCount()).isEqualTo(2L);
        assertThat(page.items()).hasSize(1);
        assertThat(hidden.totalCount()).isZero();
        assertThat(hidden.items()).isEmpty();
    }

    @Test
    void loadsOnlyActiveAnonymousResponsesFromOwnedLectures() {
        var evaluations = repository.findEvaluationResponses(
                PROFESSOR_USER_ID,
                List.of(OWNED_LECTURE_ID, OTHER_LECTURE_ID)
        );

        assertThat(evaluations).hasSize(2);
        assertThat(evaluations).allSatisfy(evaluation -> {
            assertThat(evaluation.getEnrollment().getLecture().getId()).isEqualTo(OWNED_LECTURE_ID);
            assertThat(evaluation.getEnrollment().getStatus().name()).isEqualTo("ACTIVE");
        });
        assertThat(evaluations).extracting(evaluation -> evaluation.getRatings().get("CONTENT_QUALITY"))
                .containsExactly(5, 3);
    }

    @Test
    void verifiesProfessorProfileUsingAuthenticatedUserId() {
        assertThat(repository.existsProfessorByUserId(PROFESSOR_USER_ID)).isTrue();
        assertThat(repository.existsProfessorByUserId(99999L)).isFalse();
    }

    private void insertUser(long id, String name, String role) {
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, name, role);
    }

    private void insertProfessor(long id, long userId) {
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, 98601)", id, userId);
    }

    private void insertStudent(long id, long userId) {
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 98601, 3, 2024, 'ENROLLED', 98601)",
                id, userId);
    }

    private void insertSemester(long id, int academicYear, String term, boolean current) {
        jdbcTemplate.update("INSERT INTO semesters "
                        + "(id, academic_year, term, start_date, end_date, enrollment_start_at, "
                        + "enrollment_end_at, is_current) "
                        + "VALUES (?, ?, ?, '2026-03-02', '2026-06-19', "
                        + "'2026-02-01 09:00:00', '2026-02-07 18:00:00', ?)",
                id, academicYear, term, current);
    }

    private void insertCourse(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 98601, ?, ?, 3, 3, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(long id, long courseId, long semesterId, long professorId) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, '01', 40, '공학관 301호', 'OPEN', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId, professorId);
    }

    private void insertEnrollment(long id, long studentId, long lectureId, String status) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, ?, ?, ?, ?, 'DRAFT')",
                id, studentId, lectureId, status, LocalDateTime.of(2026, 2, 2, 9, 0));
    }

    private void insertEvaluation(long id, long enrollmentId, String ratings, String comment) {
        jdbcTemplate.update("INSERT INTO lecture_evaluations "
                        + "(id, enrollment_id, ratings, comment, submitted_at) "
                        + "VALUES (?, ?, CAST(? AS JSON), ?, ?)",
                id, enrollmentId, ratings, comment, LocalDateTime.of(2026, 6, 10, 14, 30));
    }
}
