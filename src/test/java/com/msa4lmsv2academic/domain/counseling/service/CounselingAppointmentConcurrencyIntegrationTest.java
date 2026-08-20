package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.counseling.request.CounselingAppointmentCreateRequestDTO;
import com.msa4lmsv2academic.global.error.CounselingScheduleConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CounselingAppointmentConcurrencyIntegrationTest extends MySqlIntegrationTest {

    private static final long COLLEGE_ID = 93001L;
    private static final long DEPARTMENT_ID = 93001L;
    private static final long PROFESSOR_ID = 93001L;
    private static final long SECOND_PROFESSOR_ID = 93002L;
    private static final long PROFESSOR_USER_ID = 93001L;
    private static final long FIRST_STUDENT_USER_ID = 93002L;
    private static final long SECOND_STUDENT_USER_ID = 93003L;
    private static final long SECOND_PROFESSOR_USER_ID = 93004L;
    private static final long FIRST_STUDENT_ID = 93001L;
    private static final long SECOND_STUDENT_ID = 93002L;

    @Autowired
    private CounselingAppointmentService counselingAppointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LocalDateTime appointmentAt;

    @BeforeEach
    void setUp() {
        cleanUpFixture();
        appointmentAt = nextMondayAtNineThirty();

        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (?, 'COUNSEL-CONC-COL', '상담동시성대학', 1)", COLLEGE_ID);
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (?, '220', ?, '상담동시성학과', 1)", DEPARTMENT_ID, COLLEGE_ID);
        insertUser(PROFESSOR_USER_ID, "첫 번째 교수", "PROFESSOR");
        insertUser(FIRST_STUDENT_USER_ID, "첫 번째 학생", "STUDENT");
        insertUser(SECOND_STUDENT_USER_ID, "두 번째 학생", "STUDENT");
        insertUser(SECOND_PROFESSOR_USER_ID, "두 번째 교수", "PROFESSOR");
        insertProfessor(PROFESSOR_ID, PROFESSOR_USER_ID);
        insertProfessor(SECOND_PROFESSOR_ID, SECOND_PROFESSOR_USER_ID);
        insertStudent(FIRST_STUDENT_ID, FIRST_STUDENT_USER_ID, PROFESSOR_ID);
        insertStudent(SECOND_STUDENT_ID, SECOND_STUDENT_USER_ID, PROFESSOR_ID);
        insertAvailability(93001L, PROFESSOR_ID);
        insertAvailability(93002L, SECOND_PROFESSOR_ID);
    }

    @AfterEach
    void tearDown() {
        cleanUpFixture();
    }

    @Test
    void allowsOnlyOneStudentWhenTwoStudentsBookSameProfessorAtSameTime() throws Exception {
        List<BookingOutcome> outcomes = executeConcurrently(
                new BookingAttempt(FIRST_STUDENT_USER_ID, PROFESSOR_ID),
                new BookingAttempt(SECOND_STUDENT_USER_ID, PROFESSOR_ID)
        );

        assertOneSuccessAndOneConflict(outcomes);
        assertThat(countAppointmentsAt(PROFESSOR_ID, appointmentAt)).isEqualTo(1);
    }

    @Test
    void allowsOnlyOneProfessorWhenStudentBooksTwoProfessorsAtSameTime() throws Exception {
        List<BookingOutcome> outcomes = executeConcurrently(
                new BookingAttempt(FIRST_STUDENT_USER_ID, PROFESSOR_ID),
                new BookingAttempt(FIRST_STUDENT_USER_ID, SECOND_PROFESSOR_ID)
        );

        assertOneSuccessAndOneConflict(outcomes);
        assertThat(countStudentAppointmentsAt(FIRST_STUDENT_ID, appointmentAt)).isEqualTo(1);
    }

    private List<BookingOutcome> executeConcurrently(
            BookingAttempt firstAttempt,
            BookingAttempt secondAttempt
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<BookingOutcome> first = executor.submit(() -> book(firstAttempt, ready, start));
            Future<BookingOutcome> second = executor.submit(() -> book(secondAttempt, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private BookingOutcome book(
            BookingAttempt attempt,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return BookingOutcome.failure(new IllegalStateException("동시 예약 시작 신호를 받지 못했습니다."));
            }
            counselingAppointmentService.create(
                    new CounselingAppointmentCreateRequestDTO(
                            attempt.professorId(),
                            appointmentAt,
                            "동시 예약 검증"
                    ),
                    new CurrentUser(attempt.studentUserId(), "STUDENT")
            );
            return BookingOutcome.succeeded();
        } catch (Exception exception) {
            return BookingOutcome.failure(exception);
        }
    }

    private void assertOneSuccessAndOneConflict(List<BookingOutcome> outcomes) {
        assertThat(outcomes).filteredOn(BookingOutcome::success).hasSize(1);
        assertThat(outcomes)
                .filteredOn(outcome -> outcome.error() instanceof CounselingScheduleConflictException)
                .hasSize(1);
    }

    private int countAppointmentsAt(long professorId, LocalDateTime targetAppointmentAt) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM counseling_appointments "
                        + "WHERE professor_id = ? AND appointment_at = ?",
                Integer.class,
                professorId,
                targetAppointmentAt
        );
        return count == null ? 0 : count;
    }

    private int countStudentAppointmentsAt(long studentId, LocalDateTime targetAppointmentAt) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM counseling_appointments "
                        + "WHERE student_id = ? AND appointment_at = ?",
                Integer.class,
                studentId,
                targetAppointmentAt
        );
        return count == null ? 0 : count;
    }

    private void insertUser(long userId, String name, String role) {
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                userId, name, role);
    }

    private void insertProfessor(long professorId, long userId) {
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, ?)", professorId, userId, DEPARTMENT_ID);
    }

    private void insertStudent(long studentId, long userId, long advisorId) {
        jdbcTemplate.update("INSERT INTO students "
                        + "(id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                        + "VALUES (?, ?, ?, 3, 2024, 'ENROLLED', ?)",
                studentId, userId, DEPARTMENT_ID, advisorId);
    }

    private void insertAvailability(long availabilityId, long professorId) {
        jdbcTemplate.update("INSERT INTO counselor_availabilities "
                        + "(id, professor_id, day_of_week, start_time, end_time, valid_from, valid_to) "
                        + "VALUES (?, ?, ?, '09:00', '12:00', ?, NULL)",
                availabilityId,
                professorId,
                appointmentAt.getDayOfWeek().name(),
                LocalDate.now()
        );
    }

    private LocalDateTime nextMondayAtNineThirty() {
        LocalDate date = LocalDate.now().plusWeeks(2);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(9, 30);
    }

    private void cleanUpFixture() {
        jdbcTemplate.update("DELETE FROM counseling_appointments "
                + "WHERE student_id IN (?, ?) OR professor_id IN (?, ?)",
                FIRST_STUDENT_ID, SECOND_STUDENT_ID, PROFESSOR_ID, SECOND_PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM counselor_availabilities WHERE professor_id IN (?, ?)",
                PROFESSOR_ID, SECOND_PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM students WHERE id IN (?, ?)", FIRST_STUDENT_ID, SECOND_STUDENT_ID);
        jdbcTemplate.update("DELETE FROM professors WHERE id IN (?, ?)", PROFESSOR_ID, SECOND_PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?, ?, ?)",
                PROFESSOR_USER_ID, FIRST_STUDENT_USER_ID, SECOND_STUDENT_USER_ID, SECOND_PROFESSOR_USER_ID);
        jdbcTemplate.update("DELETE FROM departments WHERE id = ?", DEPARTMENT_ID);
        jdbcTemplate.update("DELETE FROM colleges WHERE id = ?", COLLEGE_ID);
    }

    private record BookingAttempt(long studentUserId, long professorId) {
    }

    private record BookingOutcome(boolean success, Exception error) {

        private static BookingOutcome succeeded() {
            return new BookingOutcome(true, null);
        }

        private static BookingOutcome failure(Exception error) {
            return new BookingOutcome(false, error);
        }
    }
}
