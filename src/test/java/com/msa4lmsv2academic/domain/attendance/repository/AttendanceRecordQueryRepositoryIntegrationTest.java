package com.msa4lmsv2academic.domain.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceRecordQueryRepositoryIntegrationTest extends MySqlIntegrationTest {

    private static final long PROFESSOR_USER_ID = 99401L;
    private static final long STUDENT_USER_ID = 99403L;

    @Autowired
    private AttendanceRecordQueryRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void createTables() {
        createAttendanceTablesForTest();
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (99401, 'ATCOL', '출결조회대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (99401, 'ATREC', 99401, '출결조회학과', 1)");
        insertUser(99401L, "담당교수", "PROFESSOR");
        insertUser(99402L, "다른교수", "PROFESSOR");
        insertUser(99403L, "조회학생", "STUDENT");
        insertUser(99404L, "다른학생", "STUDENT");
        insertProfessor(99401L, 99401L);
        insertProfessor(99402L, 99402L);
        insertStudent(99401L, 99403L, 99401L);
        insertStudent(99402L, 99404L, 99402L);
        jdbcTemplate.update("INSERT INTO semesters "
                + "(id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (99401, 2026, 'SECOND', '2026-09-01', '2026-12-18', "
                + "'2026-08-01 09:00:00', '2026-08-07 18:00:00', 1)");
        insertCourse(99401L, "AT-01", "운영체제");
        insertCourse(99402L, "AT-02", "자료구조");
        insertLecture(99401L, 99401L, 99401L);
        insertLecture(99402L, 99402L, 99402L);
        insertEnrollment(99401L, 99401L, 99401L);
        insertEnrollment(99402L, 99402L, 99402L);
        insertSession(99401L, 99401L, "2026-09-07", 2, 99401L);
        insertSession(99402L, 99401L, "2026-09-14", 2, 99401L);
        insertSession(99403L, 99402L, "2026-09-07", 3, 99402L);
        insertAttendance(99401L, 99401L, 99401L, "2026-09-07", 2, "PRESENT");
        insertAttendance(99402L, 99401L, 99402L, "2026-09-14", 2, "LATE");
        insertAttendance(99403L, 99402L, 99403L, "2026-09-07", 3, "ABSENT");
    }

    @Test
    void studentReceivesOnlyOwnAttendanceInLatestDateOrder() {
        AttendanceRecordSearchResult result = repository.search(
                STUDENT_USER_ID, UserRole.STUDENT, null, null,
                null, null, null, 0L, 20
        );

        assertThat(result.totalCount()).isEqualTo(2L);
        assertThat(result.items()).extracting(AttendanceRecordQueryResult::id)
                .containsExactly(99402L, 99401L);
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.studentName()).isEqualTo("조회학생");
            assertThat(item.courseName()).isEqualTo("운영체제");
        });
    }

    @Test
    void professorReceivesOnlyOwnedLectureAndAppliesFilters() {
        AttendanceRecordSearchResult result = repository.search(
                PROFESSOR_USER_ID, UserRole.PROFESSOR, 99401L, 99401L,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 30),
                AttendanceStatus.LATE, 0L, 20
        );

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(99402L);
            assertThat(item.classId()).isEqualTo(99401L);
            assertThat(item.status()).isEqualTo(AttendanceStatus.LATE);
        });
    }

    @Test
    void administratorReceivesAllAttendanceAndEmptyFilterReturnsClearResult() {
        AttendanceRecordSearchResult all = repository.search(
                99999L, UserRole.ADMIN, null, null,
                null, null, null, 0L, 20
        );
        AttendanceRecordSearchResult empty = repository.search(
                99999L, UserRole.ADMIN, null, null,
                null, null, AttendanceStatus.EXCUSED, 0L, 20
        );

        assertThat(all.totalCount()).isEqualTo(3L);
        assertThat(all.items()).extracting(AttendanceRecordQueryResult::id)
                .containsExactly(99402L, 99401L, 99403L);
        assertThat(empty.totalCount()).isZero();
        assertThat(empty.items()).isEmpty();
    }

    @Test
    void findsAttendanceWithRelationsForLockedUpdate() {
        var attendance = repository.findByIdForUpdate(99401L);

        assertThat(attendance).isPresent();
        assertThat(attendance.orElseThrow().getEnrollment().getLecture().getProfessor().getUser().getId())
                .isEqualTo(PROFESSOR_USER_ID);
    }

    private void createAttendanceTablesForTest() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS attendance_sessions ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "lecture_id BIGINT NOT NULL, session_date DATE NOT NULL, period INT NOT NULL, "
                + "opened_by BIGINT NOT NULL, opened_at DATETIME NOT NULL, closed_at DATETIME NULL, "
                + "status VARCHAR(20) NOT NULL, "
                + "CONSTRAINT uk_attendance_sessions_lecture_date_period "
                + "UNIQUE (lecture_id, session_date, period))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS attendances ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "enrollment_id BIGINT NOT NULL, session_id BIGINT NOT NULL, lecture_date DATE NOT NULL, "
                + "period INT NOT NULL, status VARCHAR(20) NOT NULL, remarks VARCHAR(255) NULL, "
                + "check_in_time DATETIME NULL, is_modified BOOLEAN NOT NULL DEFAULT FALSE, "
                + "created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, "
                + "CONSTRAINT uk_attendances_session_enrollment UNIQUE (session_id, enrollment_id), "
                + "INDEX idx_attendances_session_id (session_id), "
                + "INDEX idx_attendances_enrollment_id (enrollment_id))");
    }

    private void insertUser(long id, String name, String role) {
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, name, role);
    }

    private void insertProfessor(long id, long userId) {
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, 99401)", id, userId);
    }

    private void insertStudent(long id, long userId, long advisorId) {
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, 99401, 2, 2025, 'ENROLLED', ?)",
                id, userId, advisorId);
    }

    private void insertCourse(long id, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, 99401, ?, ?, 3, 2, 'MAJOR_REQUIRED')",
                id, code, name);
    }

    private void insertLecture(long id, long courseId, long professorId) {
        jdbcTemplate.update("INSERT INTO lectures "
                        + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                        + "VALUES (?, 99401, ?, ?, '01', 40, '공학관 301호', 'OPEN', "
                        + "30, 30, 30, 10, '출결조회 강의계획서')",
                id, courseId, professorId);
    }

    private void insertEnrollment(long id, long studentId, long lectureId) {
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, 'DRAFT')",
                id, studentId, lectureId, LocalDateTime.of(2026, 8, 5, 9, 0));
    }

    private void insertSession(long id, long lectureId, String date, int period, long openedBy) {
        jdbcTemplate.update("INSERT INTO attendance_sessions "
                        + "(id, lecture_id, session_date, period, opened_by, opened_at, status) "
                        + "VALUES (?, ?, ?, ?, ?, '2026-09-07 10:00:00', 'CLOSED')",
                id, lectureId, date, period, openedBy);
    }

    private void insertAttendance(
            long id,
            long enrollmentId,
            long sessionId,
            String lectureDate,
            int period,
            String status
    ) {
        jdbcTemplate.update("INSERT INTO attendances "
                        + "(id, enrollment_id, session_id, lecture_date, period, status, remarks, "
                        + "check_in_time, is_modified, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, NULL, '2026-09-07 10:02:00', 0, "
                        + "'2026-09-07 10:02:00', '2026-09-07 10:02:00')",
                id, enrollmentId, sessionId, lectureDate, period, status);
    }
}
