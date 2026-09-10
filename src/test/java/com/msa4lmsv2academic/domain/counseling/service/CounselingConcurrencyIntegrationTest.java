package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.counseling.request.CounselingCreateRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingResponseDTO;
import com.msa4lmsv2academic.global.error.CounselingStatusConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.util.List;
import java.util.concurrent.Callable;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class CounselingConcurrencyIntegrationTest extends MySqlIntegrationTest {

    private static final long COLLEGE = 700001L;
    private static final long DEPARTMENT = 700001L;
    private static final long STUDENT = 700001L;
    private static final long STUDENT_USER = 700011L;
    private static final long PROFESSOR = 700001L;
    private static final long PROFESSOR_USER = 700012L;

    @Autowired private CounselingService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        cleanFixture();
        jdbc.update("INSERT INTO colleges (id, code, name, active) VALUES (?, 'CNS-COL', '상담대학', 1)", COLLEGE);
        jdbc.update("INSERT INTO departments (id, code, college_id, name, active) VALUES (?, 'CNS', ?, '상담학과', 1)", DEPARTMENT, COLLEGE);
        jdbc.update("INSERT INTO users (id, name, role, status) VALUES (?, '상담학생', 'STUDENT', 'ACTIVE'), (?, '상담교수', 'PROFESSOR', 'ACTIVE')",
                STUDENT_USER, PROFESSOR_USER);
        jdbc.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) VALUES (?, 0, ?, 2020, ?)",
                PROFESSOR, PROFESSOR_USER, DEPARTMENT);
        jdbc.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) "
                + "VALUES (?, ?, ?, 3, 2091, 'ENROLLED', ?)", STUDENT, STUDENT_USER, DEPARTMENT, PROFESSOR);
    }

    @AfterEach
    void tearDown() {
        cleanFixture();
    }

    // 구 슬롯 예약 시스템(counseling_appointments)은 2026-09-08 온라인 Q&A 방식으로 대체돼
    // "이중 예약"이 아니라 "동시 중복 제출"이 실제 검증 대상이다.
    @Test
    void concurrentDuplicateSubmissionsBothPersistWithoutCorruption() throws Exception {
        List<CounselingResponseDTO> results = race(
                () -> create("동일 문의", "동일 내용입니다."),
                () -> create("동일 문의", "동일 내용입니다."));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isNotEqualTo(results.get(1).id());
        for (CounselingResponseDTO result : results) {
            assertThat(result.studentId()).isEqualTo(STUDENT);
            assertThat(result.professorId()).isEqualTo(PROFESSOR);
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM counselings WHERE student_id = ? AND professor_id = ?",
                Integer.class, STUDENT, PROFESSOR)).isEqualTo(2);
    }

    // findStudentByUserIdForUpdate의 PESSIMISTIC_WRITE 락이 학적 상태 변경과 실제로 직렬화되는지 검증한다.
    @Test
    void createSerializesAgainstConcurrentAcademicStatusChange() throws Exception {
        TransactionTemplate withdrawalTransaction = new TransactionTemplate(transactionManager);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Object> createOutcome = pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    return create("휴학 전 문의", "휴학 처리 전에 드리는 질문입니다.");
                } catch (CounselingStatusConflictException exception) {
                    return exception;
                }
            });
            Future<Object> statusChange = pool.submit(() -> {
                ready.countDown();
                start.await();
                return withdrawalTransaction.execute(status -> {
                    jdbc.update("UPDATE students SET academic_status = 'WITHDRAWN' WHERE id = ?", STUDENT);
                    return null;
                });
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            Object outcome = createOutcome.get(15, TimeUnit.SECONDS);
            statusChange.get(15, TimeUnit.SECONDS);

            String finalStatus = jdbc.queryForObject(
                    "SELECT academic_status FROM students WHERE id = ?", String.class, STUDENT);
            int counselingCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM counselings WHERE student_id = ?", Integer.class, STUDENT);

            assertThat(finalStatus).isEqualTo("WITHDRAWN");
            if (outcome instanceof CounselingStatusConflictException) {
                assertThat(counselingCount).isZero();
            } else {
                assertThat(outcome).isInstanceOf(CounselingResponseDTO.class);
                assertThat(counselingCount).isEqualTo(1);
            }
        }
    }

    private CounselingResponseDTO create(String title, String question) {
        return service.create(
                new CounselingCreateRequestDTO(PROFESSOR, title, question),
                new CurrentUser(STUDENT_USER, "STUDENT"));
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

    private void cleanFixture() {
        jdbc.update("DELETE FROM notifications WHERE resource_type = 'COUNSELING' AND resource_id IN "
                + "(SELECT id FROM counselings WHERE student_id = ?)", STUDENT);
        jdbc.update("DELETE FROM counselings WHERE student_id = ?", STUDENT);
        jdbc.update("DELETE FROM students WHERE id = ?", STUDENT);
        jdbc.update("DELETE FROM professors WHERE id = ?", PROFESSOR);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", STUDENT_USER, PROFESSOR_USER);
        jdbc.update("DELETE FROM departments WHERE id = ?", DEPARTMENT);
        jdbc.update("DELETE FROM colleges WHERE id = ?", COLLEGE);
    }
}
