package com.msa4lmsv2academic.domain.withdrawal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import com.msa4lmsv2academic.domain.withdrawal.request.*;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"academic.enrollment.idempotency-cleanup.cron=-", "academic.withdrawal.idempotency-cleanup.cron=-"})
@AutoConfigureMockMvc
class WithdrawalWorkflowIntegrationTest extends MySqlIntegrationTest {
    private static final CurrentUser STUDENT = new CurrentUser(180011L, "STUDENT");
    private static final CurrentUser OTHER = new CurrentUser(180012L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(180013L, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(180014L, "ADMIN");
    private static final WithdrawalAuditContext CONTEXT = new WithdrawalAuditContext("withdrawal-test", "127.0.0.1");
    private static final String URL = "/api/academic/withdrawals";
    @Autowired private WithdrawalService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private WithdrawalIdempotencyCleanupService cleanupService;
    @MockitoSpyBean private AuditLogService audit;

    @BeforeEach
    void setUp() {
        clean();
        jdbc.update("INSERT INTO colleges (id, code, name, active) VALUES (180001, 'WD-COL', '자퇴대학', 1)");
        jdbc.update("INSERT INTO departments (id, code, college_id, name, active) VALUES (180001, '981', 180001, '자퇴학과', 1)");
        jdbc.update("INSERT INTO users (id, name, role, status) VALUES "
                + "(180011, '자퇴학생', 'STUDENT', 'ACTIVE'), (180012, '다른학생', 'STUDENT', 'ACTIVE'), "
                + "(180013, '지도교수', 'PROFESSOR', 'ACTIVE'), (180014, '관리자', 'ADMIN', 'ACTIVE'), "
                + "(180015, '다른교수', 'PROFESSOR', 'ACTIVE'), (180016, '다른관리자', 'ADMIN', 'ACTIVE')");
        jdbc.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) VALUES (180001, 0, 180013, 2020, 180001)");
        jdbc.update("INSERT INTO students (id, user_id, department_id, grade_level, admission_year, academic_status, advisor_id) VALUES "
                + "(180001, 180011, 180001, 3, 2024, 'ENROLLED', 180001), (180002, 180012, 180001, 3, 2024, 'ON_LEAVE', 180001)");
    }

    @AfterEach
    void tearDown() {
        reset(auditTarget());
        clean();
    }

    @Test
    void createAndReplayPreserveUserIdentityAndProduceOneAudit() {
        var body = new WithdrawalCreateRequestDTO("  개인 사정  ", today());
        var first = service.create(body, "wd-create", STUDENT, CONTEXT);
        assertThat(first.status()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(first.reason()).isEqualTo("개인 사정");
        assertThat(first.studentId()).isEqualTo(180001L);
        assertThat(service.create(new WithdrawalCreateRequestDTO("개인 사정", today()), "wd-create", STUDENT, CONTEXT))
                .isEqualTo(first);
        assertThat(count("withdrawal_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT requester_user_id FROM idempotency_keys WHERE idempotency_key='wd-create'", Long.class))
                .isEqualTo(STUDENT.id());
        assertThatThrownBy(() -> service.create(body, "wd-create-new-key", STUDENT, CONTEXT))
                .isInstanceOf(DuplicateWithdrawalRequestException.class);
        assertThat(count("idempotency_keys")).isEqualTo(1);
    }

    @Test
    void pendingCancellationKeepsOriginalReasonAndAllowsNewApplication() {
        var first = create(null);
        var body = new WithdrawalCancelRequestDTO("  학업을 계속합니다.  ");
        var cancelled = service.cancel(first.id(), body, "wd-cancel", STUDENT, CONTEXT);
        assertThat(cancelled.status()).isEqualTo(WithdrawalStatus.CANCELLED);
        assertThat(cancelled.cancelReason()).isEqualTo("학업을 계속합니다.");
        assertThat(cancelled.reason()).isEqualTo(first.reason());
        assertThat(cancelled.cancelledBy()).isEqualTo(STUDENT.id());
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(cancelled.processedBy()).isNull();
        assertThat(service.cancel(first.id(), new WithdrawalCancelRequestDTO("학업을 계속합니다."), "wd-cancel", STUDENT, CONTEXT))
                .isEqualTo(cancelled);
        assertThat(count("audit_logs")).isEqualTo(2);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(service.create(new WithdrawalCreateRequestDTO("다시 신청", null), "wd-again", STUDENT, CONTEXT).id())
                .isNotEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT reason FROM audit_logs WHERE action='WITHDRAWAL_CANCELLED' AND actor_id=180011", String.class))
                .isEqualTo(cancelled.cancelReason());
    }

    @Test
    void cancellationAfterAdvisorApprovalPreservesReviewAndFullReason() {
        var approved = advisorApprove(create(today().plusDays(1)).id());
        var cancelled = service.cancel(approved.id(), new WithdrawalCancelRequestDTO("가".repeat(255)), "wd-cancel", STUDENT, CONTEXT);
        assertThat(cancelled.advisorReviewedBy()).isEqualTo(PROFESSOR.id());
        assertThat(cancelled.advisorReviewedAt()).isNotNull();
        assertThat(cancelled.cancelReason()).hasSize(255);
        assertThat(count("academic_status_histories")).isZero();
        assertThatThrownBy(() -> finalApprove(approved.id(), "wd-late-approval")).isInstanceOf(WithdrawalStateConflictException.class);
    }

    @Test
    void finalApprovalChangesStatusHistoryAuditAndKeyAtomicallyAndReplays() {
        var first = advisorApprove(create(today()).id());
        var result = finalApprove(first.id(), "wd-final");
        assertThat(result.effectiveDate()).isEqualTo(today());
        assertThat(result.status()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(result.processedBy()).isEqualTo(ADMIN.id());
        assertThat(studentStatus()).isEqualTo("WITHDRAWN");
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(3);
        assertThat(finalApprove(first.id(), "wd-final")).isEqualTo(result);
        assertThat(count("audit_logs")).isEqualTo(3);
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(before_value, '$.academicStatus')) FROM audit_logs "
                + "WHERE target_id=? AND action='WITHDRAWAL_APPROVED'", String.class, first.id())).isEqualTo("ENROLLED");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(after_value, '$.academicStatus')) FROM audit_logs "
                + "WHERE target_id=? AND action='WITHDRAWAL_APPROVED'", String.class, first.id())).isEqualTo("WITHDRAWN");
        assertThatThrownBy(() -> service.cancel(first.id(), new WithdrawalCancelRequestDTO("취소"), "wd-no", STUDENT, CONTEXT))
                .isInstanceOf(WithdrawalStateConflictException.class);
    }

    @Test
    void futureRequestedDateBlocksApprovalButNotAdvisorReviewRejectionOrCancellation() {
        long id = advisorApprove(create(today().plusDays(1)).id()).id();
        assertThatThrownBy(() -> finalApprove(id, "wd-too-early")).isInstanceOf(WithdrawalStateConflictException.class);
        assertThat(count("idempotency_keys")).isEqualTo(2);
        var response = service.reviewByAdmin(id, new FinalWithdrawalReviewRequestDTO(false, null, "  " + "나".repeat(500) + "  "),
                "wd-reject", ADMIN, CONTEXT);
        assertThat(response.status()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(response.rejectReason()).hasSize(500);
        assertThat(service.reviewByAdmin(id, new FinalWithdrawalReviewRequestDTO(false, null, "나".repeat(500)),
                "wd-reject", ADMIN, CONTEXT)).isEqualTo(response);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(count("academic_status_histories")).isZero();
        assertThat(jdbc.queryForObject("SELECT CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(after_value, '$.rejectReason'))) "
                + "FROM audit_logs WHERE target_id=? AND action='WITHDRAWAL_REJECTED'", Integer.class, id)).isEqualTo(500);
    }

    @Test
    void passedRequestedDateDoesNotBlockLateApprovalOrBackdateIt() {
        long id = advisorApprove(create(null).id()).id();
        jdbc.update("UPDATE withdrawal_requests SET requested_effective_date=? WHERE id=?", today().minusDays(1), id);
        assertThat(finalApprove(id, "wd-late").effectiveDate()).isEqualTo(today());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GRADUATED", "WITHDRAWN", "DISMISSED"})
    void academicStateIsRevalidatedBeforeFinalApproval(String status) {
        long id = advisorApprove(create(null).id()).id();
        jdbc.update("UPDATE students SET academic_status=? WHERE id=180001", status);
        assertThatThrownBy(() -> finalApprove(id, "wd-invalid-state")).isInstanceOf(WithdrawalStateConflictException.class);
        assertThat(service.get(id, STUDENT).status()).isEqualTo(WithdrawalStatus.ADVISOR_APPROVED);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(count("audit_logs")).isEqualTo(2);
    }

    @Test
    void auditFailureRollsBackApprovalHistoryAndReservedKey() {
        long id = advisorApprove(create(null).id()).id();
        doThrow(new IllegalStateException("forced audit failure")).when(auditTarget())
                .record(anyLong(), eq("WITHDRAWAL_APPROVED"), anyString(), anyLong(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> finalApprove(id, "wd-rollback")).isInstanceOf(IllegalStateException.class);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(service.get(id, STUDENT).status()).isEqualTo(WithdrawalStatus.ADVISOR_APPROVED);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(count("idempotency_keys")).isEqualTo(2);
    }

    @Test
    void creationAuditFailureLeavesNoBusinessOrKeyRows() {
        doThrow(new IllegalStateException("forced")).when(auditTarget()).record(anyLong(), anyString(), anyString(), anyLong(),
                any(), any(), any(), any(), any());
        assertThatThrownBy(() -> create(null)).isInstanceOf(IllegalStateException.class);
        assertThat(count("withdrawal_requests")).isZero();
        assertThat(count("idempotency_keys")).isZero();
    }

    @Test
    void keyCannotBeReusedForAnotherPayloadActorOrEndpoint() {
        long id = create(null).id();
        assertThatThrownBy(() -> service.create(new WithdrawalCreateRequestDTO("다른 사유", null), "wd-create", STUDENT, CONTEXT))
                .isInstanceOf(WithdrawalIdempotencyConflictException.class);
        assertThatThrownBy(() -> service.create(new WithdrawalCreateRequestDTO("개인 사정", null), "wd-create", OTHER, CONTEXT))
                .isInstanceOf(WithdrawalIdempotencyConflictException.class);
        assertThatThrownBy(() -> service.cancel(id, new WithdrawalCancelRequestDTO("취소"), "wd-create", STUDENT, CONTEXT))
                .isInstanceOf(WithdrawalIdempotencyConflictException.class);
        advisorApprove(id);
        finalApprove(id, "wd-final");
        assertThatThrownBy(() -> service.reviewByAdmin(id, new FinalWithdrawalReviewRequestDTO(true, today(), null),
                "wd-final", new CurrentUser(180016L, "ADMIN"), CONTEXT)).isInstanceOf(WithdrawalIdempotencyConflictException.class);
    }

    @Test
    void rolesAndOwnershipAreEnforcedForReadsAndWrites() throws Exception {
        long id = create(null).id();
        assertThatThrownBy(() -> service.cancel(id, new WithdrawalCancelRequestDTO("취소"), "wd-other", OTHER, CONTEXT))
                .isInstanceOf(WithdrawalAccessDeniedException.class);
        assertThatThrownBy(() -> service.reviewByAdvisor(id, new AdvisorWithdrawalReviewRequestDTO(true, null),
                "wd-other-prof", new CurrentUser(180015L, "PROFESSOR"), CONTEXT)).isInstanceOf(WithdrawalAccessDeniedException.class);
        mvc.perform(get(URL + "/" + id).header("X-User-Id", OTHER.id()).header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(URL + "/" + id + "/status").header("X-User-Id", ADMIN.id()).header("X-User-Role", "ADMIN")
                .header("Idempotency-Key", "wd-denied").contentType("application/json").content("{\"cancelReason\":\"취소\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header("X-User-Id", OTHER.id()).header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    void httpContractsValidateKeysDatesReasonsAndReturn201ForCreationAndReplay() throws Exception {
        mvc.perform(post(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .contentType("application/json").content("{\"reason\":\"개인 사정\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        mvc.perform(post(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .header("Idempotency-Key", "wd-http").contentType("application/json").content("{\"reason\":\"개인 사정\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        long id = jdbc.queryForObject("SELECT id FROM withdrawal_requests WHERE student_id=180001", Long.class);
        mvc.perform(post(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .header("Idempotency-Key", "wd-http").contentType("application/json").content("{\"reason\":\"개인 사정\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.status").value("PENDING"));
        assertThat(count("withdrawal_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThat(count("idempotency_keys")).isEqualTo(1);
        for (String reason : List.of("", "   ", "가".repeat(256))) {
            mvc.perform(patch(URL + "/" + id + "/status").header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                    .header("Idempotency-Key", "wd-invalid").contentType("application/json")
                    .content(mapper.writeValueAsString(java.util.Map.of("cancelReason", reason))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        }
        mvc.perform(get(URL).header("X-User-Id", ADMIN.id()).header("X-User-Role", "ADMIN").param("size", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100)).andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    void simultaneousSameKeyCreatesOneApplicationAndReturnsSameResult() throws Exception {
        var responses = race(() -> create(null), () -> create(null));
        assertThat(responses.get(0)).isEqualTo(responses.get(1));
        assertThat(count("withdrawal_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test
    void searchPreservesOneBasedPagesAndOriginalResponseShape() throws Exception {
        long oldId = create(null).id();
        service.cancel(oldId, new WithdrawalCancelRequestDTO("취소"), "wd-page-cancel", STUDENT, CONTEXT);
        long newId = service.create(new WithdrawalCreateRequestDTO("재신청", null), "wd-page-new", STUDENT, CONTEXT).id();
        mvc.perform(get(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(newId))
                .andExpect(jsonPath("$.data.totalCount").value(2)).andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1)).andExpect(jsonPath("$.data.hasNext").value(true));
        mvc.perform(get(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .param("page", "2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(oldId))
                .andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.totalCount").value(2)).andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void simultaneousApprovalAndCancellationAllowOnlyOneTerminalState() throws Exception {
        long id = advisorApprove(create(null).id()).id();
        var results = race(
                () -> outcome(() -> finalApprove(id, "wd-final-race")),
                () -> outcome(() -> service.cancel(id, new WithdrawalCancelRequestDTO("취소"), "wd-cancel-race", STUDENT, CONTEXT)));
        assertThat(results).contains("CONFLICT");
        assertThat(results.stream().filter(s -> !s.equals("CONFLICT"))).hasSize(1);
        var state = service.get(id, STUDENT).status();
        assertThat(state).isIn(WithdrawalStatus.APPROVED, WithdrawalStatus.CANCELLED);
        assertThat(count("academic_status_histories")).isEqualTo(state == WithdrawalStatus.APPROVED ? 1 : 0);
        assertThat(count("audit_logs")).isEqualTo(3);
    }

    @Test
    void expiredCreateKeyRechecksBusinessStateAndCanBeReusedAfterCancellation() {
        var original = create(null);
        jdbc.update("UPDATE idempotency_keys SET expires_at='2000-01-01' WHERE idempotency_key='wd-create'");
        assertThatThrownBy(() -> create(null)).isInstanceOf(DuplicateWithdrawalRequestException.class);
        assertThat(count("withdrawal_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
        service.cancel(original.id(), new WithdrawalCancelRequestDTO("취소"), "wd-cancel", STUDENT, CONTEXT);
        var next = create(null);
        assertThat(next.id()).isNotEqualTo(original.id());
        assertThat(next.status()).isEqualTo(WithdrawalStatus.PENDING);
        assertThat(count("idempotency_keys")).isEqualTo(2);
        assertThat(count("audit_logs")).isEqualTo(3);
    }

    @Test
    void cleanupRemovesOnlyExpiredCompletedWithdrawalKeys() {
        long id = create(null).id();
        service.cancel(id, new WithdrawalCancelRequestDTO("취소"), "wd-cancel", STUDENT, CONTEXT);
        jdbc.update("UPDATE idempotency_keys SET expires_at='2000-01-01' WHERE requester_user_id=180011");
        jdbc.update("""
                INSERT INTO idempotency_keys (idempotency_key,requester_user_id,endpoint,request_hash,status,expires_at)
                VALUES ('wd-enrollment',180011,'POST /api/academic/enrollments',?,'COMPLETED','2000-01-01'),
                       ('wd-in-progress',180011,'POST /api/academic/withdrawals',?,'IN_PROGRESS','2000-01-01')
                """, "a".repeat(64), "b".repeat(64));
        cleanupService.removeExpiredCompletedKeys();
        assertThat(jdbc.queryForList("SELECT idempotency_key FROM idempotency_keys WHERE requester_user_id=180011 ORDER BY idempotency_key", String.class))
                .containsExactly("wd-enrollment", "wd-in-progress");
        assertThat(count("withdrawal_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(2);
    }

    @Test
    void advisorRejectionReplaysAndDoesNotAllowCancellation() {
        long id = create(null).id();
        var body = new AdvisorWithdrawalReviewRequestDTO(false, "  상담 결과 반려  ");
        var result = service.reviewByAdvisor(id, body, "wd-advisor-reject", PROFESSOR, CONTEXT);
        assertThat(result.status()).isEqualTo(WithdrawalStatus.ADVISOR_REJECTED);
        assertThat(result.advisorRejectReason()).isEqualTo("상담 결과 반려");
        assertThat(service.reviewByAdvisor(id, new AdvisorWithdrawalReviewRequestDTO(false, "상담 결과 반려"),
                "wd-advisor-reject", PROFESSOR, CONTEXT)).isEqualTo(result);
        assertThatThrownBy(() -> service.cancel(id, new WithdrawalCancelRequestDTO("취소"), "wd-no", STUDENT, CONTEXT))
                .isInstanceOf(WithdrawalStateConflictException.class);
        assertThat(count("audit_logs")).isEqualTo(2);
        assertThat(count("academic_status_histories")).isZero();
    }

    @Test
    void invalidDatesAndKeysReturnE21WithoutPersistingChanges() throws Exception {
        for (String key : List.of("", "with space", "a".repeat(101))) {
            mvc.perform(post(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                    .header("Idempotency-Key", key).contentType("application/json").content("{\"reason\":\"개인 사정\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        }
        mvc.perform(post(URL).header("X-User-Id", STUDENT.id()).header("X-User-Role", "STUDENT")
                .header("Idempotency-Key", "wd-past").contentType("application/json")
                .content(mapper.writeValueAsString(new WithdrawalCreateRequestDTO("개인 사정", today().minusDays(1)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        assertThat(count("withdrawal_requests")).isZero();
        long id = advisorApprove(create(null).id()).id();
        for (LocalDate date : new LocalDate[]{null, today().minusDays(1), today().plusDays(1)}) {
            mvc.perform(patch(URL + "/" + id + "/final-review").header("X-User-Id", ADMIN.id()).header("X-User-Role", "ADMIN")
                    .header("Idempotency-Key", "wd-invalid-date").contentType("application/json")
                    .content(mapper.writeValueAsString(new FinalWithdrawalReviewRequestDTO(true, date, null))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        }
        assertThat(count("audit_logs")).isEqualTo(2);
        assertThat(count("idempotency_keys")).isEqualTo(2);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
    }

    private WithdrawalResponseDTO create(LocalDate requested) {
        return service.create(new WithdrawalCreateRequestDTO("개인 사정", requested), "wd-create", STUDENT, CONTEXT);
    }

    private AuditLogService auditTarget() {
        // 트랜잭션 프록시가 아닌 spy 대상에 stub을 설정합니다. 실제 호출은 MANDATORY 프록시를 통과합니다.
        return AopTestUtils.getUltimateTargetObject(audit);
    }

    private WithdrawalResponseDTO advisorApprove(long id) {
        return service.reviewByAdvisor(id, new AdvisorWithdrawalReviewRequestDTO(true, null), "wd-advisor", PROFESSOR, CONTEXT);
    }

    private WithdrawalResponseDTO finalApprove(long id, String key) {
        return service.reviewByAdmin(id, new FinalWithdrawalReviewRequestDTO(true, today(), null), key, ADMIN, CONTEXT);
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private int count(String table) {
        String condition = switch (table) {
            case "audit_logs" -> "actor_id BETWEEN 180011 AND 180016";
            case "idempotency_keys" -> "requester_user_id BETWEEN 180011 AND 180016";
            default -> "student_id IN (180001,180002)";
        };
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + condition, Integer.class);
    }

    private String studentStatus() {
        return jdbc.queryForObject("SELECT academic_status FROM students WHERE id=180001", String.class);
    }

    private String outcome(Callable<WithdrawalResponseDTO> operation) throws Exception {
        try {
            return operation.call().status().name();
        } catch (WithdrawalStateConflictException exception) {
            return "CONFLICT";
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
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 180011 AND 180016");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 180011 AND 180016");
        jdbc.update("DELETE FROM academic_status_histories WHERE student_id IN (180001,180002)");
        jdbc.update("DELETE FROM withdrawal_requests WHERE student_id IN (180001,180002)");
        jdbc.update("DELETE FROM students WHERE id IN (180001,180002)");
        jdbc.update("DELETE FROM professors WHERE id=180001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 180011 AND 180016");
        jdbc.update("DELETE FROM departments WHERE id=180001");
        jdbc.update("DELETE FROM colleges WHERE id=180001");
    }
}
