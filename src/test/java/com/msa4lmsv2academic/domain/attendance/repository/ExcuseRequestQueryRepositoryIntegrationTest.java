package com.msa4lmsv2academic.domain.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
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
class ExcuseRequestQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long STUDENT_USER_ID = 99303L;
    private static final long PROFESSOR_USER_ID = 99301L;

    @Autowired
    private ExcuseRequestQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99301, 'EXQCOL', '공결조회대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99301, 'EXQ', 99301, '공결조회학과', 1)");
        insertUser(99301L, "담당교수", "PROFESSOR");
        insertUser(99302L, "다른교수", "PROFESSOR");
        insertUser(99303L, "조회학생", "STUDENT");
        insertUser(99304L, "다른학생", "STUDENT");
        insertProfessor(99301L, 99301L);
        insertProfessor(99302L, 99302L);
        insertStudent(99301L, 99303L, 99301L);
        insertStudent(99302L, 99304L, 99302L);
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99301, 2026, 'SECOND', '2026-09-01', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 1)");
        insertCourse(99301L, "EXQ-01", "운영체제");
        insertCourse(99302L, "EXQ-02", "자료구조");
        insertLecture(99301L, 99301L, 99301L);
        insertLecture(99302L, 99302L, 99302L);
        insertEnrollment(99301L, 99301L, 99301L);
        insertEnrollment(99302L, 99302L, 99302L);

        insertExcuse(99301L, 99301L, "2026-09-01", "PENDING", null, "2026-09-02 10:00:00");
        insertExcuse(99302L, 99301L, "2026-09-08", "REJECTED", "증빙 불충분", "2026-09-03 10:00:00");
        insertExcuse(99303L, 99302L, "2026-09-01", "APPROVED", null, "2026-09-04 10:00:00");
    }

    @Test
    void studentReceivesOnlyOwnRequestsInNewestFirstOrder() {
        ExcuseRequestSearchResult result = repository.search(
                STUDENT_USER_ID, UserRole.STUDENT, null, 0L, 20
        );

        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.items()).extracting(ExcuseRequestQueryResult::id)
                .containsExactly(99302L, 99301L);
        assertThat(result.items().getFirst().studentName()).isEqualTo("조회학생");
        assertThat(result.items().getFirst().courseName()).isEqualTo("운영체제");
        assertThat(result.items().getFirst().rejectReason()).isEqualTo("증빙 불충분");
    }

    @Test
    void professorReceivesOnlyRequestsForOwnedLectures() {
        ExcuseRequestSearchResult result = repository.search(
                PROFESSOR_USER_ID, UserRole.PROFESSOR, null, 0L, 20
        );

        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.items()).extracting(ExcuseRequestQueryResult::id)
                .containsExactly(99302L, 99301L);
        assertThat(result.items()).allSatisfy(item ->
                assertThat(item.professorUserId()).isEqualTo(PROFESSOR_USER_ID));
    }

    @Test
    void administratorReceivesAllRequestsAndCanFilterByStatus() {
        ExcuseRequestSearchResult all = repository.search(
                99999L, UserRole.ADMIN, null, 0L, 20
        );
        ExcuseRequestSearchResult approved = repository.search(
                99999L, UserRole.ADMIN, ExcuseRequestStatus.APPROVED, 0L, 20
        );

        assertThat(all.totalCount()).isEqualTo(3L);
        assertThat(all.items()).extracting(ExcuseRequestQueryResult::id)
                .containsExactly(99303L, 99302L, 99301L);
        assertThat(approved.totalCount()).isEqualTo(1L);
        assertThat(approved.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(99303L);
            assertThat(item.status()).isEqualTo(ExcuseRequestStatus.APPROVED);
        });
    }

    @Test
    void returnsEmptyResultWhenStatusDoesNotMatch() {
        ExcuseRequestSearchResult result = repository.search(
                STUDENT_USER_ID, UserRole.STUDENT, ExcuseRequestStatus.APPROVED, 0L, 20
        );

        assertThat(result.totalCount()).isZero();
        assertThat(result.items()).isEmpty();
    }

    private void insertUser(long id, String name, String role) {
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, name, role);
    }

    private void insertProfessor(long id, long userId) {
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, 99301)", id, userId);
    }

    private void insertStudent(long id, long userId, long advisorId) {
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 99301, 2, 2025, 'ENROLLED', ?)",
                id, userId, advisorId);
    }

    private void insertCourse(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 99301, ?, ?, 3, 2, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(long id, long courseId, long professorId) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, 99301, ?, ?, '01', 40, '공학관 301호', 'OPEN', "
                        + "30, 30, 30, 10, '공결조회 강의계획서')",
                id, courseId, professorId);
    }

    private void insertEnrollment(long id, long studentId, long lectureId) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, 'DRAFT')",
                id, studentId, lectureId, LocalDateTime.of(2026, 8, 5, 9, 0));
    }

    private void insertExcuse(
            long id,
            long enrollmentId,
            String lectureDate,
            String status,
            String rejectReason,
            String createdAt
    ) {
        jdbcTemplate.update("INSERT INTO excuse_requests "
                        + "(id, enrollment_id, lecture_date, period, reason, status, reject_reason, "
                        + "attachment_original_name, attachment_stored_name, attachment_content_type, "
                        + "attachment_size, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 2, '병원 진료', ?, ?, '진료확인서.pdf', "
                        + "'excuse/status/file.pdf', 'application/pdf', 2048, ?, ?)",
                id, enrollmentId, lectureDate, status, rejectReason, createdAt, createdAt);
    }
}
