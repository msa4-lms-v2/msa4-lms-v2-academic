package com.msa4lmsv2academic.domain.enrollment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EnrollmentCartQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 92801L;
    private static final long STUDENT_ID = 92801L;
    private static final long PROFESSOR_ID = 92801L;

    @Autowired
    private EnrollmentCartQueryRepository queryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) VALUES (92801, 'CART-COL', '장바구니대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (92801, '281', 92801, '장바구니학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, '장바구니학생', 'STUDENT', 'ACTIVE')",
                STUDENT_USER_ID);
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (92802, '장바구니교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, 92802, 2020, 92801)", PROFESSOR_ID);
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 92801, 3, 2024, 'ENROLLED', ?)",
                STUDENT_ID, STUDENT_USER_ID, PROFESSOR_ID);

        insertSemester(92801L, "FIRST");
        insertSemester(92802L, "SECOND");
        insertCourse(92801L, "CART-01", "자료구조", 3);
        insertCourse(92802L, "CART-02", "운영체제", 2);
        insertLecture(92801L, 92801L, 92801L, "01");
        insertLecture(92802L, 92802L, 92802L, "01");
        jdbcTemplate.update("INSERT INTO lecture_schedules "
                + "(id, lecture_id, day_of_week, start_period, end_period) VALUES "
                + "(92801, 92801, 'MON', 1, 2), (92802, 92801, 'WED', 3, 4)");
        jdbcTemplate.update("INSERT INTO enrollment_carts (id, student_id, lecture_id, created_at) VALUES "
                + "(92801, ?, 92801, '2026-08-28 09:00:00'), (92802, ?, 92802, '2026-08-28 09:05:00')",
                STUDENT_ID, STUDENT_ID);
    }

    @Test
    void returnsFilteredItemsWithCreditsAndSchedulesForExpectedTimetable() {
        List<EnrollmentCartItemQueryResult> result = queryRepository.findByStudentUserId(
                STUDENT_USER_ID,
                (short) 2026,
                SemesterTerm.FIRST
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().cartItemId()).isEqualTo(92801L);
        assertThat(result.getFirst().courseName()).isEqualTo("자료구조");
        assertThat(result.getFirst().credits()).isEqualTo((byte) 3);
        assertThat(result.getFirst().professorName()).isEqualTo("장바구니교수");
        assertThat(result.getFirst().schedules()).hasSize(2);
        assertThat(result.getFirst().schedules()).extracting(EnrollmentCartScheduleQueryResult::dayOfWeek)
                .containsExactly(
                        com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek.MON,
                        com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek.WED
                );
    }

    @Test
    void findsOnlyOwnedCartItemAndRequiredReferences() {
        assertThat(queryRepository.findStudentByUserId(STUDENT_USER_ID)).isPresent();
        assertThat(queryRepository.findLecture(92801L)).isPresent();
        assertThat(queryRepository.existsByStudentAndLecture(STUDENT_ID, 92801L)).isTrue();
        assertThat(queryRepository.findOwnedItemForUpdate(92801L, STUDENT_ID)).isPresent();
        assertThat(queryRepository.findOwnedItemForUpdate(92801L, 99999L)).isEmpty();
    }

    @Test
    void databaseRejectsDuplicateStudentLecturePair() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO enrollment_carts (student_id, lecture_id) VALUES (?, 92801)", STUDENT_ID
        )).isInstanceOf(DuplicateKeyException.class);
    }

    private void insertSemester(long id, String term) {
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (?, 2026, ?, '2026-03-02', '2026-06-19', "
                + "'2026-08-01 09:00:00', '2026-08-31 18:00:00', 0)", id, term);
    }

    private void insertCourse(long id, String code, String name, int credits) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 92801, ?, ?, ?, 3, 'MAJOR_REQUIRED')",
                id, code, name, credits);
    }

    private void insertLecture(long id, long courseId, long semesterId, String sectionNo) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, ?, ?, ?, ?, 40, '공학관 301호', 'OPEN', 30, 30, 30, 10, NULL)",
                id, semesterId, courseId, PROFESSOR_ID, sectionNo);
    }
}
