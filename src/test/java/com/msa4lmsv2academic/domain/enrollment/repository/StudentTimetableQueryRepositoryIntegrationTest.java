package com.msa4lmsv2academic.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudentTimetableQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 93101L;
    private static final long STUDENT_ID = 93101L;
    private static final long PROFESSOR_ID = 93101L;

    @Autowired
    private StudentTimetableQueryRepository queryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (93101, 'TIME-COL', '시간표대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (93101, '311', 93101, '시간표학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (?, '시간표학생', 'STUDENT', 'ACTIVE')", STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (93102, '시간표교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, 93102, 2020, 93101)", PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 93101, 3, 2024, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);

        insertSemester(93101L, "FIRST");
        insertSemester(93102L, "SECOND");
        insertCourse(93101L, "TIME-01", "분산시스템", 3);
        insertCourse(93102L, "TIME-02", "컴파일러", 2);
        insertLecture(93101L, 93101L, 93101L, "01");
        insertLecture(93102L, 93102L, 93102L, "01");
        jdbcTemplate.update("INSERT INTO lecture_schedules "
                + "(id, lecture_id, day_of_week, start_period, end_period) VALUES "
                + "(93101, 93101, 'WED', 3, 4), (93102, 93101, 'MON', 1, 2), "
                + "(93103, 93102, 'FRI', 5, 6)");
        jdbcTemplate.update("INSERT INTO enrollments "
                + "(id, student_id, lecture_id, status, enrolled_at, grade_status) VALUES "
                + "(93101, ?, 93101, 'ACTIVE', '2026-02-10 10:00:00', 'DRAFT'), "
                + "(93102, ?, 93102, 'ACTIVE', '2026-08-10 10:00:00', 'DRAFT')",
                STUDENT_ID, STUDENT_ID);
    }

    @Test
    void returnsOnlyRequestedSemesterWithSortedSchedules() {
        List<StudentTimetableEntryQueryResult> result = queryRepository.findActiveTimetable(
                STUDENT_USER_ID,
                (short) 2026,
                SemesterTerm.FIRST
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().courseName()).isEqualTo("분산시스템");
        assertThat(result.getFirst().credits()).isEqualTo((byte) 3);
        assertThat(result.getFirst().professorName()).isEqualTo("시간표교수");
        assertThat(result.getFirst().schedules())
                .extracting(StudentTimetableScheduleQueryResult::dayOfWeek)
                .containsExactly(LectureDayOfWeek.MON, LectureDayOfWeek.WED);
    }

    @Test
    void excludesCancelledEnrollmentAndOtherStudentData() {
        jdbcTemplate.update("UPDATE enrollments SET status = 'CANCELLED' WHERE id = 93101");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (93103, '다른학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (93103, 93103, 93101, 2, 2025, 'ENROLLED', 93101)");
        jdbcTemplate.update("INSERT INTO enrollments "
                + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                + "VALUES (93103, 93103, 93101, 'ACTIVE', '2026-02-11 10:00:00', 'DRAFT')");

        List<StudentTimetableEntryQueryResult> result = queryRepository.findActiveTimetable(
                STUDENT_USER_ID,
                (short) 2026,
                SemesterTerm.FIRST
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findsAcademicStudentProfileByAuthUserId() {
        assertThat(queryRepository.existsStudentByUserId(STUDENT_USER_ID)).isTrue();
        assertThat(queryRepository.existsStudentByUserId(99999L)).isFalse();
    }

    private void insertSemester(long id, String term) {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (?, 2026, ?, '2026-03-02', '2026-06-19', "
                + "'2026-02-01 09:00:00', '2026-02-28 18:00:00', 0)", id, term);
    }

    private void insertCourse(long id, String code, String name, int credits) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 93101, ?, ?, ?, 3, 'MAJOR_REQUIRED')",
                id, code, name, credits);
    }

    private void insertLecture(long id, long courseId, long semesterId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, ?, 40, '공학관 401호', 'OPEN', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId, PROFESSOR_ID, sectionNo);
    }
}
