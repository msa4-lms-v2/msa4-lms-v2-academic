package com.msa4lmsv2academic.domain.grade.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.grade.request.GradeFinalizeRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.GradeClassResponseDTO;
import com.msa4lmsv2academic.global.error.GradeManagementConflictException;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
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
class GradeFinalizeConcurrencyIntegrationTest extends MySqlIntegrationTest {

    private static final long COLLEGE = 701001L;
    private static final long DEPARTMENT = 701001L;
    private static final long PROFESSOR = 701001L;
    private static final long PROFESSOR_USER = 701001L;
    private static final long STUDENT = 701001L;
    private static final long STUDENT_USER = 701002L;
    private static final long SEMESTER = 701001L;
    private static final long COURSE = 701001L;
    private static final long CLASS_ID = 701001L;
    private static final long ENROLLMENT_ID = 701001L;

    @Autowired private GradeManagementService service;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        cleanFixture();
        jdbc.update("INSERT INTO colleges (id, code, name, active) VALUES (?, 'GFC-COL', '성적확정대학', 1)", COLLEGE);
        jdbc.update("INSERT INTO departments (id, code, college_id, name, active) VALUES (?, 'GFC', ?, '성적확정학과', 1)", DEPARTMENT, COLLEGE);
        jdbc.update("INSERT INTO users (id, name, role, status) VALUES (?, '성적확정교수', 'PROFESSOR', 'ACTIVE'), (?, '성적확정학생', 'STUDENT', 'ACTIVE')",
                PROFESSOR_USER, STUDENT_USER);
        jdbc.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) VALUES (?, 0, ?, 2020, ?)",
                PROFESSOR, PROFESSOR_USER, DEPARTMENT);
        jdbc.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (?, ?, ?, 3, 2091, 'ENROLLED', ?)", STUDENT, STUDENT_USER, DEPARTMENT, PROFESSOR);
        jdbc.update("INSERT INTO semesters (id, academic_year, term, start_date, end_date, enrollment_start_at, enrollment_end_at, is_current) "
                        + "VALUES (?, 2091, 'FIRST', '2091-03-02', '2091-06-19', ?, ?, 0)",
                SEMESTER, LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(20));
        jdbc.update("INSERT INTO courses (id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (?, ?, 'GFC-01', '성적확정테스트', 3, 3, 'MAJOR_REQUIRED')", COURSE, DEPARTMENT);
        jdbc.update("INSERT INTO lectures (id, semester_id, course_id, professor_id, section_no, capacity, status, "
                        + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio) "
                        + "VALUES (?, ?, ?, ?, '01', 40, 'OPEN', 30, 30, 30, 10)",
                CLASS_ID, SEMESTER, COURSE, PROFESSOR);
        jdbc.update("INSERT INTO enrollments (id, student_id, lecture_id, status, enrolled_at, "
                        + "midterm_score, final_score, assignment_score, attendance_score, total_score, letter_grade, grade_status) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, 90.00, 85.00, 90.00, 100.00, 88.50, 'B+', 'DRAFT')",
                ENROLLMENT_ID, STUDENT, CLASS_ID, LocalDateTime.now().minusDays(25));
    }

    @AfterEach
    void tearDown() {
        cleanFixture();
    }

    // classId 단위 PESSIMISTIC_WRITE 락(findSyllabusByIdForUpdate/findActiveGradesForUpdate)이
    // 같은 강의에 대한 동시 확정 요청 중 하나만 성공시키고, 감사 로그·멱등성 키가 중복 생성되지 않는지 검증한다.
    @Test
    void concurrentFinalizeCallsOnlyOneSucceeds() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Object> first = pool.submit(() -> attemptFinalize("finalize-key-a", ready, start));
            Future<Object> second = pool.submit(() -> attemptFinalize("finalize-key-b", ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));

            long successCount = outcomes.stream().filter(GlobalResponseDTO.class::isInstance).count();
            long conflictCount = outcomes.stream().filter(GradeManagementConflictException.class::isInstance).count();
            assertThat(successCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
        }

        assertThat(jdbc.queryForObject(
                "SELECT grade_status FROM enrollments WHERE id = ?", String.class, ENROLLMENT_ID))
                .isEqualTo(GradeStatus.OPENED.name());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' "
                        + "AND target_id = ? AND action = 'GRADE_FINALIZED'", Integer.class, ENROLLMENT_ID))
                .isEqualTo(1);
        // 락이 이미 두 번째 요청을 "이미 확정됨" 단계에서 걸러내므로, 패자는 idempotency_keys의
        // UNIQUE 제약(reserve 단계)까지 도달하지 않는다 - 실제로 생성되는 키는 승자의 것 하나뿐이다.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys WHERE requester_user_id = ?", Integer.class, PROFESSOR_USER))
                .isEqualTo(1);
    }

    // 서버는 실제로 확정을 완료했지만 응답이 유실돼 클라이언트가 같은 멱등성 키로 재시도하는 상황을 검증한다.
    // response_snapshot이 MySQL JSON 컬럼을 거치며 BigDecimal 자릿수가 정규화되므로(90.00 -> 90.0),
    // 점수 필드는 값 기준(isEqualByComparingTo)으로, 나머지 필드는 구조적으로 비교한다.
    @Test
    void retryWithSameIdempotencyKeyAfterSuccessReplaysCachedResponse() {
        GlobalResponseDTO<GradeClassResponseDTO> first = finalize("finalize-retry-key");
        GlobalResponseDTO<GradeClassResponseDTO> retried = finalize("finalize-retry-key");

        assertThat(retried.code()).isEqualTo(first.code());
        assertThat(retried.message()).isEqualTo(first.message());
        assertThat(retried.data().classId()).isEqualTo(first.data().classId());
        assertThat(retried.data().grades()).hasSize(1);
        var firstGrade = first.data().grades().getFirst();
        var retriedGrade = retried.data().grades().getFirst();
        assertThat(retriedGrade.enrollmentId()).isEqualTo(firstGrade.enrollmentId());
        assertThat(retriedGrade.gradeStatus()).isEqualTo(firstGrade.gradeStatus());
        assertThat(retriedGrade.letterGrade()).isEqualTo(firstGrade.letterGrade());
        assertThat(retriedGrade.midtermScore()).isEqualByComparingTo(firstGrade.midtermScore());
        assertThat(retriedGrade.finalScore()).isEqualByComparingTo(firstGrade.finalScore());
        assertThat(retriedGrade.assignmentScore()).isEqualByComparingTo(firstGrade.assignmentScore());
        assertThat(retriedGrade.attendanceScore()).isEqualByComparingTo(firstGrade.attendanceScore());
        assertThat(retriedGrade.totalScore()).isEqualByComparingTo(firstGrade.totalScore());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' "
                        + "AND target_id = ? AND action = 'GRADE_FINALIZED'", Integer.class, ENROLLMENT_ID))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM idempotency_keys WHERE requester_user_id = ?", Integer.class, PROFESSOR_USER))
                .isEqualTo(1);
    }

    private Object attemptFinalize(String idempotencyKey, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return finalize(idempotencyKey);
        } catch (GradeManagementConflictException exception) {
            return exception;
        }
    }

    private GlobalResponseDTO<GradeClassResponseDTO> finalize(String idempotencyKey) {
        return service.finalizeGrades(
                CLASS_ID, new GradeFinalizeRequestDTO(GradeStatus.OPENED), idempotencyKey,
                new CurrentUser(PROFESSOR_USER, "PROFESSOR"), "trace-" + idempotencyKey, "127.0.0.1");
    }

    private void cleanFixture() {
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id = ?", PROFESSOR_USER);
        jdbc.update("DELETE FROM audit_logs WHERE target_type = 'ENROLLMENT_GRADE' AND target_id = ?", ENROLLMENT_ID);
        jdbc.update("DELETE FROM enrollments WHERE id = ?", ENROLLMENT_ID);
        jdbc.update("DELETE FROM lectures WHERE id = ?", CLASS_ID);
        jdbc.update("DELETE FROM courses WHERE id = ?", COURSE);
        jdbc.update("DELETE FROM semesters WHERE id = ?", SEMESTER);
        jdbc.update("DELETE FROM students WHERE id = ?", STUDENT);
        jdbc.update("DELETE FROM professors WHERE id = ?", PROFESSOR);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", PROFESSOR_USER, STUDENT_USER);
        jdbc.update("DELETE FROM departments WHERE id = ?", DEPARTMENT);
        jdbc.update("DELETE FROM colleges WHERE id = ?", COLLEGE);
    }
}
