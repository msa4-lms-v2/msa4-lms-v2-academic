package com.msa4lmsv2academic.domain.lecture.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
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
class ProfessorLectureQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long PROFESSOR_USER_ID = 94001L;
    private static final long PROFESSOR_ID = 94001L;

    @Autowired
    private ProfessorLectureQueryRepository professorLectureQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (94001, 'PROF-LECT-COL', '교수강의조회대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (94001, 'PROF-LECT-DEPT', 94001, '교수강의조회학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (94001, '담당교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (94002, '다른교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (94003, '수강학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (94001, 0, 94001, 2020, 94001)");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (94002, 0, 94002, 2021, 94001)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (94001, 94003, 94001, 2, 2025, 'ENROLLED', 94001)");

        insertSemester(94001L, "FIRST");
        insertSemester(94002L, "SECOND");
        insertCourse(94001L, "PROF-LECT-01", "자료구조");
        insertCourse(94002L, "PROF-LECT-02", "운영체제");
        insertCourse(94003L, "PROF-LECT-03", "다른교수강의");
        insertLecture(94001L, 94001L, 94001L, PROFESSOR_ID, "01", "OPEN");
        insertLecture(94002L, 94002L, 94002L, PROFESSOR_ID, "01", "CLOSED");
        insertLecture(94003L, 94003L, 94001L, 94002L, "01", "OPEN");

        insertSchedule(94001L, 94001L, "WED", 3, 4);
        insertSchedule(94002L, 94001L, "MON", 1, 2);
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (94001, 94001, 94001, 'ACTIVE', ?, 'DRAFT')",
                LocalDateTime.of(2026, 2, 2, 9, 0));
    }

    @Test
    void returnsOnlyOwnedLecturesMatchingFiltersWithSchedulesAndEnrollmentCount() {
        ProfessorLectureSearchResult result = professorLectureQueryRepository.searchByProfessorUserId(
                PROFESSOR_USER_ID,
                (short) 2026,
                SemesterTerm.FIRST,
                LectureStatus.OPEN,
                null,
                0L,
                20
        );

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
        ProfessorLectureQueryResult lecture = result.items().getFirst();
        assertThat(lecture.classId()).isEqualTo(94001L);
        assertThat(lecture.courseName()).isEqualTo("자료구조");
        assertThat(lecture.professorName()).isEqualTo("담당교수");
        assertThat(lecture.departmentName()).isEqualTo("교수강의조회학과");
        assertThat(lecture.currentEnrollmentCount()).isEqualTo(1L);
        assertThat(lecture.schedules())
                .extracting(ProfessorLectureScheduleQueryResult::dayOfWeek)
                .containsExactly(LectureDayOfWeek.MON, LectureDayOfWeek.WED);
    }

    @Test
    void excludesAnotherProfessorLecturesAndAppliesPagination() {
        ProfessorLectureSearchResult result = professorLectureQueryRepository.searchByProfessorUserId(
                PROFESSOR_USER_ID,
                null,
                null,
                null,
                null,
                0L,
                1
        );

        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().classId()).isEqualTo(94002L);
    }

    @Test
    void returnsEmptyResultWhenNoLectureMatches() {
        ProfessorLectureSearchResult result = professorLectureQueryRepository.searchByProfessorUserId(
                PROFESSOR_USER_ID,
                (short) 2025,
                null,
                null,
                null,
                0L,
                20
        );

        assertThat(result.totalCount()).isZero();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void verifiesAcademicProfessorProfileByAuthenticatedUserId() {
        assertThat(professorLectureQueryRepository.existsProfessorByUserId(PROFESSOR_USER_ID)).isTrue();
        assertThat(professorLectureQueryRepository.existsProfessorByUserId(99999L)).isFalse();
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
                        + "VALUES (?, 94001, ?, ?, 3, 2, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(
            long id,
            long courseId,
            long semesterId,
            long professorId,
            String sectionNo,
            String status
    ) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, ?, 40, '공학관 301호', ?, 30, 30, 30, 10, '강의계획서')",
                id, semesterId, courseId, professorId, sectionNo, status);
    }

    private void insertSchedule(long id, long lectureId, String dayOfWeek, int startPeriod, int endPeriod) {
        jdbcTemplate.update("INSERT INTO lecture_schedules "
                        + "(id, lecture_id, day_of_week, start_period, end_period) VALUES (?, ?, ?, ?, ?)",
                id, lectureId, dayOfWeek, startPeriod, endPeriod);
    }
}
