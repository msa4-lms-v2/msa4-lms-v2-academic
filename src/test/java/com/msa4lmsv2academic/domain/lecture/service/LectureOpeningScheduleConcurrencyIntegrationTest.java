package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningReviewRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningScheduleRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.global.error.DuplicateLectureOpeningRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
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
class LectureOpeningScheduleConcurrencyIntegrationTest extends MySqlIntegrationTest {

    private static final long COLLEGE_ID = 95001L;
    private static final long DEPARTMENT_ID = 95001L;
    private static final long PROFESSOR_USER_ID = 95001L;
    private static final long ADMIN_USER_ID = 95002L;
    private static final long PROFESSOR_ID = 95001L;
    private static final long SEMESTER_ID = 95001L;
    private static final long FIRST_COURSE_ID = 95001L;
    private static final long SECOND_COURSE_ID = 95002L;
    private static final CurrentUser PROFESSOR = new CurrentUser(PROFESSOR_USER_ID, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_USER_ID, "ADMIN");

    @Autowired
    private LectureOpeningService lectureOpeningService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long firstRequestId;
    private Long secondRequestId;

    @BeforeEach
    void setUp() {
        cleanUpFixture();
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (?, 'OPEN-CONC-COL', '개설동시성대학', 1)", COLLEGE_ID);
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (?, '240', ?, '개설동시성학과', 1)", DEPARTMENT_ID, COLLEGE_ID);
        insertUser(PROFESSOR_USER_ID, "동시승인 교수", "PROFESSOR");
        insertUser(ADMIN_USER_ID, "동시승인 관리자", "ADMIN");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (?, 0, ?, 2020, ?)", PROFESSOR_ID, PROFESSOR_USER_ID, DEPARTMENT_ID);
        jdbcTemplate.update("INSERT INTO semesters "
                        + "(id, academic_year, term, start_date, end_date, enrollment_start_at, "
                        + "enrollment_end_at, is_current) VALUES (?, 2027, 'FIRST', ?, ?, ?, ?, 0)",
                SEMESTER_ID,
                LocalDate.of(2027, 3, 2),
                LocalDate.of(2027, 6, 18),
                LocalDateTime.of(2027, 2, 1, 9, 0),
                LocalDateTime.of(2027, 2, 7, 18, 0));
        insertCourse(FIRST_COURSE_ID, "OPEN-CONC-1", "분산시스템 심화");
        insertCourse(SECOND_COURSE_ID, "OPEN-CONC-2", "클라우드 컴퓨팅");

        firstRequestId = lectureOpeningService.create(createRequest(FIRST_COURSE_ID), PROFESSOR)
                .openingRequestId();
        secondRequestId = lectureOpeningService.create(createRequest(SECOND_COURSE_ID), PROFESSOR)
                .openingRequestId();
    }

    @AfterEach
    void tearDown() {
        cleanUpFixture();
    }

    @Test
    void allowsOnlyOneApprovalForSameProfessorAndOverlappingTime() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<ReviewOutcome> first = executor.submit(() -> review(firstRequestId, ready, start));
            Future<ReviewOutcome> second = executor.submit(() -> review(secondRequestId, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ReviewOutcome> outcomes = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );

            assertThat(outcomes).filteredOn(ReviewOutcome::success).hasSize(1);
            assertThat(outcomes)
                    .filteredOn(outcome -> outcome.error() instanceof DuplicateLectureOpeningRequestException)
                    .hasSize(1);
            assertThat(countApprovedLectures()).isEqualTo(1);
            assertThat(countApprovedSchedules()).isEqualTo(1);
        } finally {
            start.countDown();
        }
    }

    private ReviewOutcome review(Long requestId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return ReviewOutcome.failure(new IllegalStateException("동시 승인 시작 신호가 없습니다."));
            }
            LectureOpeningResponseDTO response = lectureOpeningService.review(
                    new LectureOpeningReviewRequestDTO(requestId, true, null, null),
                    ADMIN
            );
            return ReviewOutcome.succeeded(response);
        } catch (Exception exception) {
            return ReviewOutcome.failure(exception);
        }
    }

    private LectureOpeningCreateRequestDTO createRequest(long courseId) {
        return new LectureOpeningCreateRequestDTO(
                courseId,
                SEMESTER_ID,
                "01",
                40,
                "공학관 501호",
                30,
                30,
                30,
                10,
                "동시 승인 시간 충돌 검증용 강의계획서",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.MON,
                        (byte) 2,
                        (byte) 3
                ))
        );
    }

    private int countApprovedLectures() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lectures WHERE semester_id = ? AND professor_id = ?",
                Integer.class,
                SEMESTER_ID,
                PROFESSOR_ID
        );
        return count == null ? 0 : count;
    }

    private int countApprovedSchedules() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lecture_schedules schedule "
                        + "JOIN lectures lecture ON lecture.id = schedule.lecture_id "
                        + "WHERE lecture.semester_id = ? AND lecture.professor_id = ?",
                Integer.class,
                SEMESTER_ID,
                PROFESSOR_ID
        );
        return count == null ? 0 : count;
    }

    private void insertUser(long userId, String name, String role) {
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) VALUES (?, ?, ?, 'ACTIVE')",
                userId, name, role);
    }

    private void insertCourse(long courseId, String code, String name) {
        jdbcTemplate.update("INSERT INTO courses "
                        + "(id, department_id, code, name, credits, target_grade, completion_type) "
                        + "VALUES (?, ?, ?, ?, 3, 3, 'MAJOR_REQUIRED')",
                courseId, DEPARTMENT_ID, code, name);
    }

    private void cleanUpFixture() {
        jdbcTemplate.update("DELETE FROM audit_logs WHERE target_type IN ('LECTURE_OPENING_REQUEST', 'LECTURE')");
        jdbcTemplate.update("DELETE FROM lecture_schedules WHERE lecture_id IN "
                + "(SELECT id FROM lectures WHERE semester_id = ? AND professor_id = ?)",
                SEMESTER_ID, PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM lectures WHERE semester_id = ? AND professor_id = ?",
                SEMESTER_ID, PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM lecture_opening_request_schedules WHERE request_id IN "
                + "(SELECT id FROM lecture_opening_requests WHERE semester_id = ? AND professor_id = ?)",
                SEMESTER_ID, PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM lecture_opening_requests WHERE semester_id = ? AND professor_id = ?",
                SEMESTER_ID, PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM courses WHERE id IN (?, ?)", FIRST_COURSE_ID, SECOND_COURSE_ID);
        jdbcTemplate.update("DELETE FROM semesters WHERE id = ?", SEMESTER_ID);
        jdbcTemplate.update("DELETE FROM professors WHERE id = ?", PROFESSOR_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", PROFESSOR_USER_ID, ADMIN_USER_ID);
        jdbcTemplate.update("DELETE FROM departments WHERE id = ?", DEPARTMENT_ID);
        jdbcTemplate.update("DELETE FROM colleges WHERE id = ?", COLLEGE_ID);
    }

    private record ReviewOutcome(boolean success, LectureOpeningResponseDTO response, Exception error) {

        private static ReviewOutcome succeeded(LectureOpeningResponseDTO response) {
            return new ReviewOutcome(true, response, null);
        }

        private static ReviewOutcome failure(Exception error) {
            return new ReviewOutcome(false, null, error);
        }
    }
}
