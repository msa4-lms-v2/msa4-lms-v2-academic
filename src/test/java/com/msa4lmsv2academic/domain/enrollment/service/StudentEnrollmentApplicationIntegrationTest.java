package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentHistoryRepository;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentHistory;
import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCreateResponseDTO;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.error.InvalidEnrollmentApplicationRequestException;
import com.msa4lmsv2academic.global.error.StudentEnrollmentAccessDeniedException;
import com.msa4lmsv2academic.global.idempotency.AcademicIdempotencyKeyRepository;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = "academic.enrollment.idempotency-cleanup.cron=-")
@AutoConfigureMockMvc
class StudentEnrollmentApplicationIntegrationTest extends MySqlIntegrationTest {
    private static final long STUDENT = 120001L;
    private static final long USER = 120011L;
    private static final long LECTURE = 120001L;
    private static final String URL = "/api/academic/enrollments";

    @Autowired private StudentEnrollmentApplicationService service;
    @Autowired private EnrollmentIdempotencyCleanupService cleanupService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private EnrollmentHistoryRepository historyRepository;
    @MockitoSpyBean private AcademicIdempotencyKeyRepository keyRepository;

    @BeforeEach
    void setUp() {
        cleanFixture();
        jdbc.update("INSERT INTO colleges (id, code, name, active) VALUES (120001, 'APP-COL', '신청대학', 1)");
        jdbc.update("INSERT INTO departments (id, code, college_id, name, active) VALUES (120001, '991', 120001, '신청학과', 1)");
        jdbc.update("INSERT INTO users (id, name, role, status) VALUES "
                + "(120011, '신청학생', 'STUDENT', 'ACTIVE'), (120012, '다른학생', 'STUDENT', 'ACTIVE'), "
                + "(120013, '신청교수', 'PROFESSOR', 'ACTIVE')");
        jdbc.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) VALUES (120001, 0, 120013, 2020, 120001)");
        jdbc.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) VALUES "
                + "(120001, 120011, 120001, 3, 2089, 'ENROLLED', 120001), "
                + "(120002, 120012, 120001, 3, 2089, 'ENROLLED', 120001)");
        jdbc.update("INSERT INTO semesters (id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                + "VALUES (120001, 2091, 'FIRST', '2091-03-02', '2091-06-19', ?, ?, 0)",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        jdbc.update("INSERT INTO enrollment_credit_limit_rules (id, semester_id, max_credits, is_active) VALUES (120001, 120001, 6, 1)");
        for (long id = 120001; id <= 120003; id++) {
            jdbc.update("INSERT INTO courses (id, department_id, code, name, credits, target_grade, completion_type) "
                    + "VALUES (?, 120001, ?, '신청교과목', 3, 3, 'MAJOR_REQUIRED')", id, "APP-" + id);
            jdbc.update("INSERT INTO lectures (id, semester_id, course_id, professor_id, section_no, capacity, status, "
                    + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio) "
                    + "VALUES (?, 120001, ?, 120001, '01', 40, 'OPEN', 30, 30, 30, 10)", id, id);
        }
    }

    @AfterEach
    void tearDown() {
        reset(historyRepository, keyRepository);
        cleanFixture();
    }

    @Test
    void newTablesExplicitlyUseUtf8mb4AndPreserveKeyCollation() {
        var charsets = jdbc.queryForList("SELECT c.CHARACTER_SET_NAME FROM information_schema.TABLES t "
                + "JOIN information_schema.COLLATION_CHARACTER_SET_APPLICABILITY c "
                + "ON c.COLLATION_NAME = t.TABLE_COLLATION "
                + "WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME IN ('enrollment_histories', 'idempotency_keys')",
                String.class);
        assertThat(charsets).containsExactlyInAnyOrder("utf8mb4", "utf8mb4");
        assertThat(jdbc.queryForObject("SELECT COLLATION_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'idempotency_keys' AND COLUMN_NAME = 'idempotency_key'",
                String.class)).isEqualTo("utf8mb4_bin");
    }

    @Test
    void createsEnrollmentHistoryAndJsonSnapshotAtomicallyAndReplaysExactResult() {
        var response = apply(USER, LECTURE, "success");
        assertThat(response.code()).isEqualTo("00");
        assertThat(response.data().studentId()).isEqualTo(STUDENT);
        assertThat(response.data().status().name()).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT grade_status FROM enrollments WHERE id = ?", String.class,
                response.data().enrollmentId())).isEqualTo("DRAFT");
        assertCounts(1, 1, 1);
        assertThat(jdbc.queryForObject("SELECT action FROM enrollment_histories WHERE student_id = 120001", String.class)).isEqualTo("ENROLL");
        assertThat(jdbc.queryForObject("SELECT TIMESTAMPDIFF(SECOND, created_at, expires_at) FROM idempotency_keys "
                + "WHERE idempotency_key = 'success'", Long.class)).isEqualTo(86400);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(response_snapshot, '$.code')) "
                + "FROM idempotency_keys WHERE idempotency_key = 'success'", String.class)).isEqualTo("00");
        assertThat(apply(USER, LECTURE, "success")).isEqualTo(response);
        assertCounts(1, 1, 1);
    }

    @Test
    void completedReplayDoesNotRevalidatePeriodOrAcademicState() {
        var response = apply(USER, LECTURE, "replay");
        jdbc.update("UPDATE semesters SET enrollment_end_at = ? WHERE id = 120001", LocalDateTime.now().minusHours(1));
        jdbc.update("UPDATE students SET academic_status = 'ON_LEAVE' WHERE id = 120001");
        assertThat(apply(USER, LECTURE, "replay")).isEqualTo(response);
        assertCounts(1, 1, 1);
    }

    @Test
    void cancelledRowAndGradesRemainAndNewKeyCreatesNewEnrollment() {
        var first = apply(USER, LECTURE, "old");
        jdbc.update("UPDATE enrollments SET status = 'CANCELLED', grade_status = 'OPENED', letter_grade = 'A' WHERE id = ?",
                first.data().enrollmentId());
        assertThat(apply(USER, LECTURE, "old")).isEqualTo(first);
        var second = apply(USER, LECTURE, "new");
        assertThat(second.data().enrollmentId()).isNotEqualTo(first.data().enrollmentId());
        assertThat(jdbc.queryForObject("SELECT letter_grade FROM enrollments WHERE id = ?", String.class,
                first.data().enrollmentId())).isEqualTo("A");
        assertThat(jdbc.queryForObject("SELECT letter_grade FROM enrollments WHERE id = ?", String.class,
                second.data().enrollmentId())).isNull();
        assertCounts(2, 2, 2);
    }

    @Test
    void differentPayloadOrStudentCannotReuseCompletedKey() {
        apply(USER, LECTURE, "conflict");
        rejected(USER, 120002, "conflict", "IDEMPOTENCY_KEY_CONFLICT");
        rejected(120012, LECTURE, "conflict", "IDEMPOTENCY_KEY_CONFLICT");
        assertCounts(1, 1, 1);
    }

    @Test
    void otherEndpointAndInProgressKeysAreNotReplayed() {
        insertKey("other", "POST /api/academic/other", "COMPLETED", LocalDateTime.now().plusDays(1));
        insertKey("processing", EnrollmentIdempotencyService.ENDPOINT, "IN_PROGRESS", LocalDateTime.now().plusDays(1));
        rejected(USER, LECTURE, "other", "IDEMPOTENCY_KEY_CONFLICT");
        rejected(USER, LECTURE, "processing", "IDEMPOTENCY_KEY_CONFLICT");
        assertCounts(0, 0, 2);
    }

    @ParameterizedTest
    @CsvSource({"ON_LEAVE,STUDENT_ON_LEAVE", "WITHDRAWN,STUDENT_WITHDRAWN",
            "GRADUATED,STUDENT_GRADUATED", "DISMISSED,STUDENT_DISMISSED"})
    void rejectsAcademicStatesAndRollsBackReservedKey(String state, String reason) {
        jdbc.update("UPDATE students SET academic_status = ? WHERE id = 120001", state);
        rejected(USER, LECTURE, "state", reason);
        assertCounts(0, 0, 0);
        jdbc.update("UPDATE students SET academic_status = 'ENROLLED' WHERE id = 120001");
        apply(USER, LECTURE, "state");
        assertCounts(1, 1, 1);
    }

    @Test
    void rejectsClosedLecture() {
        jdbc.update("UPDATE lectures SET status = 'CLOSED' WHERE id = 120001");
        rejected(USER, LECTURE, "closed", "LECTURE_NOT_OPEN");
        assertCounts(0, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsOutsideEnrollmentWindow(boolean future) {
        jdbc.update("UPDATE semesters SET enrollment_start_at = ?, enrollment_end_at = ? WHERE id = 120001",
                LocalDateTime.now().plusDays(future ? 1 : -2), LocalDateTime.now().plusDays(future ? 2 : -1));
        rejected(USER, LECTURE, "period", "ENROLLMENT_PERIOD_CLOSED");
        assertCounts(0, 0, 0);
    }

    @Test
    void rejectsDuplicateUnderNewKeyEvenAfterOriginalKeyExpires() {
        apply(USER, LECTURE, "duplicate");
        rejected(USER, LECTURE, "different", "DUPLICATE_ENROLLMENT");
        jdbc.update("UPDATE idempotency_keys SET expires_at = ? WHERE idempotency_key = 'duplicate'", LocalDateTime.now().minusSeconds(1));
        rejected(USER, LECTURE, "duplicate", "DUPLICATE_ENROLLMENT");
        cleanupService.removeExpiredCompletedKeys();
        assertCounts(1, 1, 0);
    }

    @Test
    void expiredCompletedKeyCanBeUsedForNewRequest() {
        var first = apply(USER, LECTURE, "expired");
        jdbc.update("UPDATE idempotency_keys SET expires_at = ? WHERE idempotency_key = 'expired'", LocalDateTime.now().minusSeconds(1));
        var second = apply(USER, 120002, "expired");
        assertThat(second.data().enrollmentId()).isNotEqualTo(first.data().enrollmentId());
        assertCounts(2, 2, 1);
    }

    @Test
    void cleanupOnlyDeletesExpiredCompletedEnrollmentKeys() {
        apply(USER, LECTURE, "expired");
        jdbc.update("UPDATE idempotency_keys SET expires_at = ? WHERE idempotency_key = 'expired'", LocalDateTime.now().minusSeconds(1));
        insertKey("other", "POST /api/academic/other", "COMPLETED", LocalDateTime.now().minusDays(1));
        insertKey("processing", EnrollmentIdempotencyService.ENDPOINT, "IN_PROGRESS", LocalDateTime.now().minusDays(1));
        insertKey("valid", EnrollmentIdempotencyService.ENDPOINT, "COMPLETED", LocalDateTime.now().plusDays(1));
        cleanupService.removeExpiredCompletedKeys();
        assertCounts(1, 1, 3);
    }

    @Test
    void rejectsFullLecture() {
        jdbc.update("UPDATE lectures SET capacity = 1 WHERE id = 120001");
        apply(USER, LECTURE, "seat");
        rejected(120012, LECTURE, "no-seat", "CAPACITY_EXCEEDED");
        assertCounts(1, 1, 1);
    }

    @Test
    void overlappingInclusivePeriodsRejectButAdjacentPeriodsAllow() {
        jdbc.update("INSERT INTO lecture_schedules (lecture_id, day_of_week, start_period, end_period) VALUES "
                + "(120001, 'MON', 1, 3), (120002, 'MON', 3, 4), (120003, 'MON', 4, 5)");
        apply(USER, LECTURE, "first");
        rejected(USER, 120002, "overlap", "SCHEDULE_CONFLICT");
        apply(USER, 120003, "adjacent");
        assertCounts(2, 2, 2);
    }

    @Test
    void cancelledSchedulesDoNotBlockNewApplication() {
        jdbc.update("INSERT INTO lecture_schedules (lecture_id, day_of_week, start_period, end_period) VALUES "
                + "(120001, 'MON', 1, 3), (120002, 'MON', 1, 3)");
        var first = apply(USER, LECTURE, "first");
        jdbc.update("UPDATE enrollments SET status = 'CANCELLED' WHERE id = ?", first.data().enrollmentId());
        apply(USER, 120002, "second");
        assertCounts(2, 2, 2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void missingOrInactiveCreditRuleRejectsWithoutFallback(boolean missing) {
        if (missing) {
            jdbc.update("DELETE FROM enrollment_credit_limit_rules WHERE id = 120001");
        } else {
            jdbc.update("UPDATE enrollment_credit_limit_rules SET is_active = 0 WHERE id = 120001");
        }
        rejected(USER, LECTURE, "missing-rule", "CREDIT_LIMIT_RULE_NOT_CONFIGURED");
        assertCounts(0, 0, 0);
    }

    @Test
    void exactCreditLimitAllowsAndExcessRollsBack() {
        apply(USER, LECTURE, "one");
        apply(USER, 120002, "two");
        rejected(USER, 120003, "three", "CREDIT_LIMIT_EXCEEDED");
        assertCounts(2, 2, 2);
    }

    @Test
    void prerequisiteRejectionRollsBackAndSameKeyCanRetryAfterPassingGrade() {
        jdbc.update("INSERT INTO course_prerequisites (course_id, prerequisite_course_id, is_active) VALUES (120002, 120001, 1)");
        rejected(USER, 120002, "prerequisite", "PREREQUISITE_NOT_COMPLETED");
        assertCounts(0, 0, 0);
        var prior = apply(USER, LECTURE, "prior");
        jdbc.update("UPDATE enrollments SET grade_status = 'OPENED', letter_grade = 'A' WHERE id = ?", prior.data().enrollmentId());
        apply(USER, 120002, "prerequisite");
        assertCounts(2, 2, 2);
    }

    @Test
    void historyFailureRollsBackEnrollmentAndKey() {
        doThrow(new IllegalStateException("injected history failure")).when(historyRepository).saveAndFlush(any(EnrollmentHistory.class));
        assertThatThrownBy(() -> apply(USER, LECTURE, "failure")).isInstanceOf(IllegalStateException.class);
        assertCounts(0, 0, 0);
    }

    @Test
    void snapshotFailureRollsBackEnrollmentHistoryAndKey() {
        doThrow(new IllegalStateException("injected snapshot flush failure")).when(keyRepository).flush();
        assertThatThrownBy(() -> apply(USER, LECTURE, "failure")).isInstanceOf(IllegalStateException.class);
        assertCounts(0, 0, 0);
    }

    @Test
    void concurrentSameStudentCannotExceedCreditLimit() throws Exception {
        jdbc.update("UPDATE enrollment_credit_limit_rules SET max_credits = 3 WHERE id = 120001");
        assertThat(race(() -> outcome(USER, LECTURE, "one"), () -> outcome(USER, 120002, "two")))
                .containsExactlyInAnyOrder("SUCCESS", "CREDIT_LIMIT_EXCEEDED");
        assertCounts(1, 1, 1);
    }

    @Test
    void concurrentStudentsCannotTakeSameLastSeat() throws Exception {
        jdbc.update("UPDATE lectures SET capacity = 1 WHERE id = 120001");
        assertThat(race(() -> outcome(USER, LECTURE, "one"), () -> outcome(120012, LECTURE, "two")))
                .containsExactlyInAnyOrder("SUCCESS", "CAPACITY_EXCEEDED");
        assertCounts(1, 1, 1);
    }

    @Test
    void concurrentSameKeyReplaysOneSuccessfulApplication() throws Exception {
        var results = race(() -> apply(USER, LECTURE, "same"), () -> apply(USER, LECTURE, "same"));
        assertThat(results.get(0)).isEqualTo(results.get(1));
        assertCounts(1, 1, 1);
    }

    @Test
    void concurrentDifferentStudentsSharingKeyHaveOneWinner() throws Exception {
        assertThat(race(() -> outcome(USER, LECTURE, "shared"), () -> outcome(120012, 120002, "shared")))
                .containsExactlyInAnyOrder("SUCCESS", "IDEMPOTENCY_KEY_CONFLICT");
        assertCounts(1, 1, 1);
    }

    @Test
    void concurrentDifferentKeysForSameLectureCreateOnlyOneEnrollment() throws Exception {
        assertThat(race(() -> outcome(USER, LECTURE, "one"), () -> outcome(USER, LECTURE, "two")))
                .containsExactlyInAnyOrder("SUCCESS", "DUPLICATE_ENROLLMENT");
        assertCounts(1, 1, 1);
    }

    @Test
    void opaqueKeysAreCaseSensitive() {
        apply(USER, LECTURE, "Case-Key");
        apply(USER, 120002, "case-key");
        assertCounts(2, 2, 2);
    }

    @Test
    void httpPostExposesDetailedReasonsOnlyForBusinessRejections() throws Exception {
        jdbc.update("UPDATE students SET academic_status = 'ON_LEAVE' WHERE id = 120001");
        mvc.perform(studentPost().header("Idempotency-Key", "http").content("{\"lectureId\":120001}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E11"))
                .andExpect(jsonPath("$.message").value("이미 처리되었거나 현재 상태와 충돌합니다."))
                .andExpect(jsonPath("$.data.reasons[0].code").value("STUDENT_ON_LEAVE"));
        mvc.perform(studentPost().content("{\"lectureId\":120001}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"))
                .andExpect(jsonPath("$.data").isEmpty());
        assertCounts(0, 0, 0);
    }

    @Test
    void httpPostAndReplayReturn200AndSameId() throws Exception {
        var result = mvc.perform(studentPost().header("Idempotency-Key", "http").content("{\"lectureId\":120001}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.studentId").value(120001)).andReturn();
        mvc.perform(studentPost().header("Idempotency-Key", "http").content("{\"lectureId\":120001}"))
                .andExpect(status().isOk()).andExpect(content().json(result.getResponse().getContentAsString()));
        assertCounts(1, 1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"lectureId\":0}", "{\"lectureId\":-1}", "{\"lectureId\":120001,\"studentId\":120002}"})
    void invalidOrForgedBodiesReturn400(String body) throws Exception {
        mvc.perform(studentPost().header("Idempotency-Key", "invalid").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        assertCounts(0, 0, 0);
    }

    @Test
    void missingStudentOrLectureReturns404WithoutKey() throws Exception {
        mvc.perform(studentPost().header("Idempotency-Key", "missing").content("{\"lectureId\":99999999}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E10"));
        mvc.perform(post(URL).header("X-User-Id", "99999999").header("X-User-Role", "STUDENT")
                        .header("Idempotency-Key", "missing").contentType("application/json").content("{\"lectureId\":120001}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E10"));
        assertCounts(0, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "PROFESSOR"})
    void otherRolesReturn403(String role) throws Exception {
        mvc.perform(post(URL).header("X-User-Id", USER).header("X-User-Role", role)
                        .header("Idempotency-Key", "forbidden").contentType("application/json").content("{\"lectureId\":120001}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E03"));
        assertCounts(0, 0, 0);
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mvc.perform(post(URL).header("Idempotency-Key", "anonymous").contentType("application/json").content("{\"lectureId\":120001}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void directCallsAlsoValidateRoleAndKey() {
        assertThatThrownBy(() -> service.create(new StudentEnrollmentCreateRequestDTO(LECTURE), "role", new CurrentUser(USER, "ADMIN")))
                .isInstanceOf(StudentEnrollmentAccessDeniedException.class);
        for (String key : List.of("", " ", "a".repeat(101), "with space")) {
            assertThatThrownBy(() -> apply(USER, LECTURE, key)).isInstanceOf(InvalidEnrollmentApplicationRequestException.class);
        }
        assertCounts(0, 0, 0);
    }

    private GlobalResponseDTO<StudentEnrollmentCreateResponseDTO> apply(long userId, long lectureId, String key) {
        return service.create(new StudentEnrollmentCreateRequestDTO(lectureId), key, new CurrentUser(userId, "STUDENT"));
    }

    private void rejected(long userId, long lectureId, String key, String reason) {
        assertThatThrownBy(() -> apply(userId, lectureId, key)).isInstanceOfSatisfying(
                EnrollmentApplicationRejectedException.class,
                exception -> assertThat(exception.getReasons()).extracting("code").contains(reason));
    }

    private String outcome(long userId, long lectureId, String key) {
        try {
            apply(userId, lectureId, key);
            return "SUCCESS";
        } catch (EnrollmentApplicationRejectedException exception) {
            return exception.getReasons().getFirst().code();
        }
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var b = pool.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
        }
    }

    private MockHttpServletRequestBuilder studentPost() {
        return post(URL).header("X-User-Id", USER).header("X-User-Role", "STUDENT").contentType("application/json");
    }

    private void insertKey(String key, String endpoint, String status, LocalDateTime expiresAt) {
        jdbc.update("INSERT INTO idempotency_keys (idempotency_key, requester_student_id, endpoint, request_hash, response_snapshot, status, expires_at) "
                + "VALUES (?, 120001, ?, ?, '{}', ?, ?)", key, endpoint, "a".repeat(64), status, expiresAt);
    }

    private void assertCounts(int enrollments, int histories, int keys) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM enrollments WHERE student_id IN (120001,120002)", Integer.class)).isEqualTo(enrollments);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM enrollment_histories WHERE student_id IN (120001,120002)", Integer.class)).isEqualTo(histories);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_keys WHERE requester_student_id IN (120001,120002)", Integer.class)).isEqualTo(keys);
    }

    private void cleanFixture() {
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_student_id IN (120001,120002)");
        jdbc.update("DELETE FROM enrollment_histories WHERE student_id IN (120001,120002)");
        jdbc.update("DELETE FROM enrollments WHERE student_id IN (120001,120002)");
        jdbc.update("DELETE FROM lecture_schedules WHERE lecture_id BETWEEN 120001 AND 120003");
        jdbc.update("DELETE FROM course_prerequisites WHERE course_id BETWEEN 120001 AND 120003");
        jdbc.update("DELETE FROM lectures WHERE id BETWEEN 120001 AND 120003");
        jdbc.update("DELETE FROM courses WHERE id BETWEEN 120001 AND 120003");
        jdbc.update("DELETE FROM enrollment_credit_limit_rules WHERE id = 120001");
        jdbc.update("DELETE FROM semesters WHERE id = 120001");
        jdbc.update("DELETE FROM students WHERE id IN (120001,120002)");
        jdbc.update("DELETE FROM professors WHERE id = 120001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 120011 AND 120013");
        jdbc.update("DELETE FROM departments WHERE id = 120001");
        jdbc.update("DELETE FROM colleges WHERE id = 120001");
    }
}
