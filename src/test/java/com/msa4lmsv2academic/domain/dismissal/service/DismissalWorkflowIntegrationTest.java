package com.msa4lmsv2academic.domain.dismissal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.dismissal.entity.*;
import com.msa4lmsv2academic.domain.dismissal.request.*;
import com.msa4lmsv2academic.domain.dismissal.response.DismissalResponseDTO;
import com.msa4lmsv2academic.domain.withdrawal.service.*;
import com.msa4lmsv2academic.domain.withdrawal.request.*;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"academic.dismissal.idempotency-cleanup.cron=-",
        "academic.leave.idempotency-cleanup.cron=-", "academic.withdrawal.idempotency-cleanup.cron=-",
        "academic.enrollment.idempotency-cleanup.cron=-"})
@AutoConfigureMockMvc
class DismissalWorkflowIntegrationTest extends MySqlIntegrationTest {
    private static final long STUDENT_ID = 285001L;
    private static final CurrentUser STUDENT = new CurrentUser(285011L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(285013L, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(285014L, "ADMIN");
    private static final CurrentUser OTHER_ADMIN = new CurrentUser(285015L, "ADMIN");
    private static final DismissalAuditContext CONTEXT = new DismissalAuditContext("dismissal-test", "127.0.0.1");
    private static final WithdrawalAuditContext WD_CONTEXT = new WithdrawalAuditContext("dismissal-test", "127.0.0.1");
    private static final String URL = "/api/academic/dismissals";
    @Autowired private DismissalService service;
    @Autowired private DismissalIdempotencyCleanupService cleanup;
    @Autowired private WithdrawalService withdrawals;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoSpyBean private AuditLogService audit;

    @BeforeEach void setUp() {
        clean();
        jdbc.update("INSERT INTO colleges (id,code,name,active) VALUES (285001,'DM-COL','제적테스트대학',1)");
        jdbc.update("INSERT INTO departments (id,code,college_id,name,active) VALUES (285001,'985',285001,'제적테스트학과',1)");
        jdbc.update("INSERT INTO users (id,name,role,status) VALUES (285011,'제적학생','STUDENT','ACTIVE'),"
                + "(285012,'다른학생','STUDENT','ACTIVE'),(285013,'지도교수','PROFESSOR','ACTIVE'),"
                + "(285014,'관리자','ADMIN','ACTIVE'),(285015,'다른관리자','ADMIN','ACTIVE')");
        jdbc.update("INSERT INTO professors (id,version,user_id,hire_year,department_id) VALUES (285001,0,285013,2020,285001)");
        jdbc.update("INSERT INTO students (id,user_id,department_id,grade_level,admission_year,academic_status,advisor_id) VALUES "
                + "(285001,285011,285001,2,2025,'ENROLLED',285001),(285002,285012,285001,2,2025,'ENROLLED',285001)");
    }

    @AfterEach void tearDown() {
        reset(auditTarget());
        clean();
    }

    @Test void createReplayAndDuplicateAcrossReasons() {
        var first = create("dm-create");
        assertThat(first.status()).isEqualTo(DismissalStatus.PENDING);
        assertThat(first.version()).isZero();
        assertThat(first.registeredBy()).isEqualTo(ADMIN.id());
        assertThat(first.processedBy()).isNull();
        assertThat(first.processedAt()).isNull();
        assertThat(first.cancelReason()).isNull();
        assertThat(create("dm-create")).isEqualTo(first);
        assertThat(count("dismissal_candidates")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThatThrownBy(() -> service.create(new DismissalCreateRequestDTO(STUDENT_ID,
                DismissalReasonType.NON_REGISTRATION, "다른 사유"), "dm-other-reason", ADMIN, CONTEXT))
                .isInstanceOf(DismissalConflictException.class);
    }

    @Test void updateVersionReplayRejectsOldScreenAndPreservesRegistrant() {
        var first = create("dm-create");
        var request = new DismissalUpdateRequestDTO(0L, DismissalReasonType.ACADEMIC_WARNING, "보".repeat(500));
        var updated = service.update(first.id(), request, "dm-update", OTHER_ADMIN, CONTEXT);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.registeredBy()).isEqualTo(ADMIN.id());
        assertThat(updated.reason()).hasSize(500);
        assertThat(updated.studentId()).isEqualTo(first.studentId());
        assertThat(service.update(first.id(), request, "dm-update", OTHER_ADMIN, CONTEXT)).isEqualTo(updated);
        assertThatThrownBy(() -> confirm(first.id(), 0, "dm-stale")).isInstanceOf(DismissalConflictException.class);
        assertThat(jdbc.queryForObject("SELECT CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(after_value,'$.reason'))) "
                + "FROM audit_logs WHERE actor_id=285015 AND action='DISMISSAL_UPDATED'", Integer.class)).isEqualTo(500);
        assertThat(confirm(first.id(), 1, "dm-confirm").version()).isEqualTo(2);
    }

    @Test void cancelStoresMetadataWithoutChangingStatusHistoryThenAllowsNewCandidate() {
        var first = create("dm-create");
        var request = new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "취".repeat(500));
        var result = service.changeStatus(first.id(), request, "dm-cancel", OTHER_ADMIN, CONTEXT);
        assertThat(result.status()).isEqualTo(DismissalStatus.CANCELLED);
        assertThat(result.processedBy()).isEqualTo(OTHER_ADMIN.id());
        assertThat(result.processedAt()).isNotNull();
        assertThat(result.cancelReason()).hasSize(500);
        assertThat(result.reason()).isEqualTo(first.reason());
        assertThat(service.changeStatus(first.id(), request, "dm-cancel", OTHER_ADMIN, CONTEXT)).isEqualTo(result);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThatThrownBy(() -> service.update(first.id(), new DismissalUpdateRequestDTO(1L,
                DismissalReasonType.DISCIPLINARY, "종결 수정"), "dm-terminal", ADMIN, CONTEXT))
                .isInstanceOf(DismissalConflictException.class);
        assertThat(create("dm-new").id()).isNotEqualTo(first.id());
    }

    @ParameterizedTest
    @CsvSource({"GENERAL_LEAVE,PENDING", "GENERAL_RETURN,PENDING", "MILITARY_LEAVE,PENDING", "MILITARY_RETURN,PENDING",
            "GENERAL_LEAVE,ADVISOR_APPROVED", "GENERAL_RETURN,ADVISOR_APPROVED",
            "MILITARY_LEAVE,ADVISOR_APPROVED", "MILITARY_RETURN,ADVISOR_APPROVED"})
    void confirmCancelsAllPendingTypesAndPreservesOriginals(String leaveType, String withdrawalStatus) {
        long withdrawalId = pendingWithdrawal("ADVISOR_APPROVED".equals(withdrawalStatus));
        seedLeave(leaveType);
        var first = create("dm-create");
        var result = confirm(first.id(), first.version(), "dm-confirm");
        assertThat(result.status()).isEqualTo(DismissalStatus.CONFIRMED);
        assertThat(result.academicStatus().name()).isEqualTo("DISMISSED");
        assertThat(result.processedBy()).isEqualTo(ADMIN.id());
        assertThat(result.cancelReason()).isNull();
        assertThat(confirm(first.id(), 0, "dm-confirm")).isEqualTo(result);
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT source_id FROM academic_status_histories WHERE student_id=285001", Long.class))
                .isEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT reason FROM academic_status_histories WHERE student_id=285001", String.class))
                .isEqualTo("관리자 제적 확정");
        assertThat(jdbc.queryForObject("SELECT status FROM academic_requests WHERE student_id=285001", String.class)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT attachment_stored_name FROM academic_requests WHERE student_id=285001", String.class))
                .isEqualTo("leave-requests/proof.pdf");
        var withdrawal = withdrawals.get(withdrawalId, ADMIN);
        assertThat(withdrawal.status().name()).isEqualTo("CANCELLED");
        assertThat(withdrawal.reason()).isEqualTo("자퇴 원본");
        assertThat(withdrawal.cancelledBy()).isEqualTo(ADMIN.id());
        if ("ADVISOR_APPROVED".equals(withdrawalStatus)) assertThat(withdrawal.advisorReviewedBy()).isEqualTo(PROFESSOR.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=285014 AND "
                + "action IN ('LEAVE_DISMISSAL_CANCELLED','WITHDRAWAL_DISMISSAL_CANCELLED') "
                + "AND JSON_EXTRACT(after_value,'$.dismissalId')=?", Integer.class, first.id())).isEqualTo(2);
        assertThatThrownBy(() -> create("dm-already-dismissed")).isInstanceOf(DismissalConflictException.class);
    }

    @Test void pastTerminalApplicationsArePreserved() {
        var withdrawalId = pendingWithdrawal(false);
        withdrawals.cancel(withdrawalId, new WithdrawalCancelRequestDTO("스스로 취소"), "dm-wd-cancel", STUDENT, WD_CONTEXT);
        seedLeave("GENERAL_LEAVE");
        jdbc.update("UPDATE academic_requests SET status='APPROVED' WHERE student_id=285001");
        var first = create("dm-create");
        confirm(first.id(), 0, "dm-confirm");
        assertThat(withdrawals.get(withdrawalId, ADMIN).cancelReason()).isEqualTo("스스로 취소");
        assertThat(jdbc.queryForObject("SELECT status FROM academic_requests WHERE student_id=285001", String.class)).isEqualTo("APPROVED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISMISSAL_CONFIRMED", "LEAVE_DISMISSAL_CANCELLED", "WITHDRAWAL_DISMISSAL_CANCELLED"})
    void failedAuditRollsBackEveryRelatedChange(String failedAction) {
        pendingWithdrawal(true);
        seedLeave("GENERAL_LEAVE");
        var first = create("dm-create");
        int beforeAudits = count("audit_logs");
        doThrow(new IllegalStateException("forced rollback")).when(auditTarget()).record(anyLong(), eq(failedAction),
                anyString(), anyLong(), any(), any(), anyString(), any(), any());
        assertThatThrownBy(() -> confirm(first.id(), 0, "dm-failed")).isInstanceOf(IllegalStateException.class);
        assertThat(service.get(first.id(), ADMIN).status()).isEqualTo(DismissalStatus.PENDING);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(count("academic_status_histories")).isZero();
        assertThat(count("audit_logs")).isEqualTo(beforeAudits);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_keys WHERE idempotency_key='dm-failed'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM academic_requests WHERE student_id=285001", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT status FROM withdrawal_requests WHERE student_id=285001", String.class)).isEqualTo("ADVISOR_APPROVED");
    }

    @Test void pendingDismissalBlocksOnlyFinalWithdrawalApprovalAndCancelUnblocks() {
        var first = create("dm-create");
        long withdrawalId = pendingWithdrawal(true); // 후보가 있어도 신청과 지도교수 승인 유지
        assertThatThrownBy(() -> finalWithdrawal(withdrawalId, "dm-wd-final")).isInstanceOf(WithdrawalStateConflictException.class);
        service.changeStatus(first.id(), new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "자퇴로 처리"),
                "dm-cancel", ADMIN, CONTEXT);
        finalWithdrawal(withdrawalId, "dm-wd-final");
        assertThat(studentStatus()).isEqualTo("WITHDRAWN");
        assertThatThrownBy(() -> create("dm-after-withdrawal")).isInstanceOf(DismissalConflictException.class);
    }

    @Test void withdrawalRejectionIsStillAllowedWithPendingDismissal() {
        long id = pendingWithdrawal(true);
        create("dm-create");
        var result = withdrawals.reviewByAdmin(id, new FinalWithdrawalReviewRequestDTO(false, null, "보완 필요"),
                "dm-wd-reject", ADMIN, WD_CONTEXT);
        assertThat(result.status().name()).isEqualTo("REJECTED");
        assertThat(studentStatus()).isEqualTo("ENROLLED");
    }

    @Test void expiredLeaveCandidateCannotConfirmAfterReturnButCanCancel() {
        assertThatThrownBy(() -> service.create(new DismissalCreateRequestDTO(STUDENT_ID, DismissalReasonType.LEAVE_EXPIRED, "만료"),
                "dm-leave-create", ADMIN, CONTEXT)).isInstanceOf(DismissalConflictException.class);
        jdbc.update("UPDATE students SET academic_status='ON_LEAVE' WHERE id=285001");
        var first = service.create(new DismissalCreateRequestDTO(STUDENT_ID, DismissalReasonType.LEAVE_EXPIRED, "만료"),
                "dm-leave-create", ADMIN, CONTEXT);
        jdbc.update("UPDATE students SET academic_status='ENROLLED' WHERE id=285001");
        assertThatThrownBy(() -> confirm(first.id(), 0, "dm-returned")).isInstanceOf(DismissalConflictException.class);
        assertThat(service.changeStatus(first.id(), new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "복학 완료"),
                "dm-cancel", ADMIN, CONTEXT).status()).isEqualTo(DismissalStatus.CANCELLED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GRADUATED", "WITHDRAWN", "DISMISSED"})
    void terminalStudentStatesBlockCreateAndConfirmation(String state) {
        var first = create("dm-create");
        jdbc.update("UPDATE students SET academic_status=? WHERE id=285001", state);
        assertThatThrownBy(() -> confirm(first.id(), 0, "dm-terminal-confirm")).isInstanceOf(DismissalConflictException.class);
        service.changeStatus(first.id(), new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "학적 변경"),
                "dm-cancel", ADMIN, CONTEXT);
        assertThatThrownBy(() -> create("dm-terminal-create")).isInstanceOf(DismissalConflictException.class);
    }

    @Test void sameKeyCannotReplayForDifferentActorBodyOrEndpoint() {
        var first = create("dm-create");
        assertThatThrownBy(() -> service.create(createBody(), "dm-create", OTHER_ADMIN, CONTEXT)).isInstanceOf(DismissalConflictException.class);
        assertThatThrownBy(() -> service.create(new DismissalCreateRequestDTO(285002L,
                DismissalReasonType.DISCIPLINARY, "다른 학생"), "dm-create", ADMIN, CONTEXT)).isInstanceOf(DismissalConflictException.class);
        assertThatThrownBy(() -> confirm(first.id(), 0, "dm-create")).isInstanceOf(DismissalConflictException.class);
        assertThatThrownBy(() -> service.create(createBody(), "dm-create", STUDENT, CONTEXT)).isInstanceOf(DismissalAccessDeniedException.class);
    }

    @Test void keysAreCaseSensitiveAndCleanupIsScopedToExpiredCompletedDismissals() {
        var first = create("DM-CREATE");
        service.changeStatus(first.id(), new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "취소"),
                "dm-create", ADMIN, CONTEXT);
        assertThat(count("idempotency_keys")).isEqualTo(2);
        jdbc.update("UPDATE idempotency_keys SET expires_at=? WHERE requester_user_id=285014", DismissalPolicy.now().minusDays(1));
        jdbc.update("INSERT INTO idempotency_keys (idempotency_key,requester_user_id,endpoint,request_hash,status,expires_at)"
                + " VALUES ('dm-other-endpoint',285014,'POST /api/academic/withdrawals','hash','COMPLETED',?),"
                + "('dm-in-progress',285014,'POST /api/academic/dismissals','hash','IN_PROGRESS',?)",
                DismissalPolicy.now().minusDays(1), DismissalPolicy.now().minusDays(1));
        cleanup.cleanExpired();
        assertThat(count("idempotency_keys")).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT idempotency_key FROM idempotency_keys WHERE requester_user_id=285014", String.class))
                .containsExactlyInAnyOrder("dm-other-endpoint", "dm-in-progress");
    }

    @Test void concurrentCreationAllowsOnePendingCandidate() throws Exception {
        assertThat(race(() -> outcome(() -> create("dm-race-a")), () -> outcome(() -> create("dm-race-b"))))
                .containsExactlyInAnyOrder("OK", "CONFLICT");
        assertThat(count("dismissal_candidates")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test void concurrentSameKeyCreateReplaysSameIdentity() throws Exception {
        var results = race(() -> create("dm-race").id(), () -> create("dm-race").id());
        assertThat(results.getFirst()).isEqualTo(results.getLast());
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test void concurrentUpdateAndConfirmCannotConfirmStaleContents() throws Exception {
        var first = create("dm-create");
        assertThat(race(() -> outcome(() -> service.update(first.id(), new DismissalUpdateRequestDTO(0L,
                        DismissalReasonType.DISCIPLINARY, "변경 근거"), "dm-edit", ADMIN, CONTEXT)),
                () -> outcome(() -> confirm(first.id(), 0, "dm-confirm")))).containsExactlyInAnyOrder("OK", "CONFLICT");
        var current = service.get(first.id(), ADMIN);
        assertThat(current.version()).isEqualTo(1);
        if (current.status() == DismissalStatus.CONFIRMED) assertThat(current.reason()).isEqualTo(first.reason());
        else assertThat(count("academic_status_histories")).isZero();
    }

    @Test void concurrentCancelAndConfirmHaveSingleWinner() throws Exception {
        var first = create("dm-create");
        assertThat(race(() -> outcome(() -> service.changeStatus(first.id(),
                new DismissalStatusRequestDTO(0L, DismissalStatus.CANCELLED, "취소"), "dm-cancel", ADMIN, CONTEXT)),
                () -> outcome(() -> confirm(first.id(), 0, "dm-confirm")))).containsExactlyInAnyOrder("OK", "CONFLICT");
        assertThat(count("audit_logs")).isEqualTo(2);
    }

    @Test void concurrentWithdrawalApprovalAndCandidateCreationDoNotOverwriteTerminalState() throws Exception {
        long withdrawalId = pendingWithdrawal(true);
        var results = race(() -> outcome(() -> create("dm-create")), () -> {
            try { finalWithdrawal(withdrawalId, "dm-wd-final"); return "OK"; }
            catch (WithdrawalStateConflictException failure) { return "CONFLICT"; }
        });
        assertThat(results).containsExactlyInAnyOrder("OK", "CONFLICT");
        if (studentStatus().equals("WITHDRAWN")) assertThat(count("dismissal_candidates")).isZero();
        else assertThat(count("dismissal_candidates")).isEqualTo(1);
    }

    @Test void httpRolesValidationFiltersPagingAndPrivacy() throws Exception {
        var first = create("dm-create");
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        for (var actor : List.of(STUDENT, PROFESSOR)) {
            mvc.perform(as(get(URL), actor)).andExpect(status().isForbidden());
            mvc.perform(as(get(URL + "/" + first.id()), actor)).andExpect(status().isForbidden());
            mvc.perform(as(post(URL), actor).contentType("application/json").content(mapper.writeValueAsString(createBody())))
                    .andExpect(status().isForbidden());
            mvc.perform(as(put(URL + "/" + first.id()), actor).contentType("application/json").content(
                    mapper.writeValueAsString(new DismissalUpdateRequestDTO(0L, DismissalReasonType.DISCIPLINARY, "변경"))))
                    .andExpect(status().isForbidden());
            mvc.perform(as(patch(URL + "/" + first.id() + "/status"), actor).contentType("application/json")
                    .content(mapper.writeValueAsString(new DismissalStatusRequestDTO(0L, DismissalStatus.CONFIRMED, null))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(as(get(URL), ADMIN).param("studentId","285001").param("departmentId","285001")
                        .param("studentName","제적").param("reasonType","DISCIPLINARY").param("status","PENDING").param("size","101"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.page").value(1)).andExpect(jsonPath("$.data.size").value(100));
        mvc.perform(as(get(URL), ADMIN).param("status","CANCELLED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty()).andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(as(get(URL), ADMIN).param("page","0")).andExpect(status().isBadRequest());
        mvc.perform(as(get(URL), ADMIN).param("reasonType","WITHDRAWN")).andExpect(status().isBadRequest());
        mvc.perform(as(get(URL+"/999999999"),ADMIN)).andExpect(status().isNotFound());
        confirm(first.id(),0,"dm-confirm");
        mvc.perform(as(get("/api/academic/status-histories"),STUDENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].sourceType").value("DISMISSAL"))
                .andExpect(jsonPath("$.data.items[0].reason").value("관리자 제적 확정"));
    }

    @Test void httpCreateUpdateConfirmAndInvalidInputs() throws Exception {
        mvc.perform(as(post(URL),ADMIN).contentType("application/json").content(mapper.writeValueAsString(createBody())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E21"));
        var result = mvc.perform(as(post(URL),ADMIN).header("Idempotency-Key","dm-http").contentType("application/json")
                .content(mapper.writeValueAsString(createBody()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING")).andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(as(put(URL+"/"+id),ADMIN).header("Idempotency-Key","dm-http-invalid").contentType("application/json")
                .content("{\"reasonType\":\"DISCIPLINARY\",\"reason\":\"누락 버전\"}")).andExpect(status().isBadRequest());
        mvc.perform(as(put(URL+"/"+id),ADMIN).header("Idempotency-Key","dm-http-update").contentType("application/json")
                .content(mapper.writeValueAsString(new DismissalUpdateRequestDTO(0L,DismissalReasonType.DISCIPLINARY,"수정 근거"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(as(patch(URL+"/"+id+"/status"),ADMIN).header("Idempotency-Key","dm-http-cancel").contentType("application/json")
                .content(mapper.writeValueAsString(new DismissalStatusRequestDTO(1L,DismissalStatus.CANCELLED," "))))
                .andExpect(status().isBadRequest());
        mvc.perform(as(patch(URL+"/"+id+"/status"),ADMIN).header("Idempotency-Key","dm-http-stale").contentType("application/json")
                .content(mapper.writeValueAsString(new DismissalStatusRequestDTO(0L,DismissalStatus.CONFIRMED,null))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E11"));
        mvc.perform(as(patch(URL+"/"+id+"/status"),ADMIN).header("Idempotency-Key","dm-http-confirm").contentType("application/json")
                .content(mapper.writeValueAsString(new DismissalStatusRequestDTO(1L,DismissalStatus.CONFIRMED,null))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test void deletedStudentsAreNotExposedAndCountsMatchVisibleRows() {
        var first = create("dm-create");
        jdbc.update("UPDATE users SET deleted_at=CURRENT_TIMESTAMP WHERE id=285011");
        assertThatThrownBy(() -> service.get(first.id(), ADMIN)).isInstanceOf(DismissalNotFoundException.class);
        var filter = new DismissalSearchRequestDTO(1,20,STUDENT_ID,null,null,null,null,null);
        var result = service.search(filter,ADMIN,PageRequest.of(0,20));
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThatThrownBy(() -> confirm(first.id(),0,"dm-deleted")).isInstanceOf(DismissalNotFoundException.class);
    }

    @Test void expiredCreateKeyCanBeReusedAfterCandidateCancellation() {
        var first = create("dm-expired");
        service.changeStatus(first.id(),new DismissalStatusRequestDTO(0L,DismissalStatus.CANCELLED,"취소"),
                "dm-cancel",ADMIN,CONTEXT);
        jdbc.update("UPDATE idempotency_keys SET expires_at=? WHERE idempotency_key='dm-expired'",
                DismissalPolicy.now().minusDays(1));
        assertThat(create("dm-expired").id()).isNotEqualTo(first.id());
    }

    @Test void schemaConstraintsAndMigrationPreserveRows() {
        var first = create("dm-create");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO dismissal_candidates (student_id,reason_type,reason,registered_by) "
                + "VALUES (285001,'DISCIPLINARY','중복',285014)")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE dismissal_candidates SET status='CANCELLED' WHERE id=?",first.id()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("ck_dismissal_candidates_processing");
        assertThatThrownBy(() -> jdbc.update("UPDATE dismissal_candidates SET reason_type='WITHDRAWAL' WHERE id=?",first.id()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("ck_dismissal_candidates_reason_type");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("migration/20260828_create_dismissal_candidates.sql")).execute(jdbc.getDataSource());
        assertThat(service.get(first.id(),ADMIN).reason()).isEqualTo(first.reason());
    }

    @Test void openApiDocumentsEveryOperationAndPreservesWithdrawal201() throws Exception {
        var result = mvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn();
        var document = mapper.readTree(result.getResponse().getContentAsString());
        var paths = document.path("paths");
        for (String[] op : List.of(new String[]{URL,"get"},new String[]{URL,"post"},
                new String[]{URL+"/{id}","get"},new String[]{URL+"/{id}","put"},new String[]{URL+"/{id}/status","patch"})) {
            var operation = paths.path(op[0]).path(op[1]);
            assertThat(operation.path("operationId").asString()).doesNotContainIgnoringCase("scrum");
            assertThat(operation.path("summary").asString()).isNotBlank();
            assertThat(operation.path("description").asString()).contains("ADMIN");
            assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
            assertThat(operation.path("responses").has("200")).isTrue();
            for(String code : List.of("400","401","403","404","409","500")) assertThat(operation.path("responses").has(code)).isTrue();
        }
        assertThat(paths.path("/api/academic/withdrawals").path("post").path("responses").has("201")).isTrue();
        var schemas = document.path("components").path("schemas");
        assertThat(schemas.path("DismissalUpdateRequestDTO").path("required").toString()).contains("version","reasonType","reason");
        assertThat(schemas.path("DismissalResponseDTO").path("properties").has("processedAt")).isTrue();
    }

    private DismissalCreateRequestDTO createBody() { return new DismissalCreateRequestDTO(STUDENT_ID,DismissalReasonType.DISCIPLINARY,"비공개 징계 근거"); }
    private DismissalResponseDTO create(String key) { return service.create(createBody(),key,ADMIN,CONTEXT); }
    private DismissalResponseDTO confirm(long id,long version,String key) {
        return service.changeStatus(id,new DismissalStatusRequestDTO(version,DismissalStatus.CONFIRMED,null),key,ADMIN,CONTEXT);
    }
    private long pendingWithdrawal(boolean approve) {
        var result = withdrawals.create(new WithdrawalCreateRequestDTO("자퇴 원본",null),"dm-wd-create",STUDENT,WD_CONTEXT);
        if(approve) withdrawals.reviewByAdvisor(result.id(),new AdvisorWithdrawalReviewRequestDTO(true,null),"dm-wd-advisor",PROFESSOR,WD_CONTEXT);
        return result.id();
    }
    private void finalWithdrawal(long id,String key) {
        withdrawals.reviewByAdmin(id,new FinalWithdrawalReviewRequestDTO(true,DismissalPolicy.now().toLocalDate(),null),key,ADMIN,WD_CONTEXT);
    }
    private void seedLeave(String type) {
        jdbc.update("INSERT INTO academic_requests (student_id,request_type,reason,target_year,target_semester,return_year,return_semester,"
                + "attachment_original_name,attachment_stored_name,attachment_content_type,attachment_size) "
                + "VALUES (285001,?,'원본 사유',2026,2,2028,2,'proof.pdf','leave-requests/proof.pdf','application/pdf',100)",type);
    }
    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request,CurrentUser actor) {
        return request.header("X-User-Id",actor.id()).header("X-User-Role",actor.role());
    }
    private String studentStatus() { return jdbc.queryForObject("SELECT academic_status FROM students WHERE id=285001",String.class); }
    private int count(String table) {
        String column = switch(table) { case "audit_logs" -> "actor_id"; case "idempotency_keys" -> "requester_user_id"; default -> "student_id"; };
        return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+column+" BETWEEN 285001 AND 285015",Integer.class);
    }
    private AuditLogService auditTarget() { return AopTestUtils.getUltimateTargetObject(audit); }
    private String outcome(Callable<?> operation) throws Exception {
        try { operation.call(); return "OK"; } catch(DismissalConflictException failure) { return "CONFLICT"; }
    }
    private <T> List<T> race(Callable<T> a,Callable<T> b) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try(var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { ready.countDown(); start.await(); return a.call(); });
            var second = pool.submit(() -> { ready.countDown(); start.await(); return b.call(); });
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
        }
    }
    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 285011 AND 285015");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 285011 AND 285015");
        jdbc.update("DELETE FROM academic_status_histories WHERE student_id IN (285001,285002)");
        jdbc.update("DELETE FROM dismissal_candidates WHERE student_id IN (285001,285002)");
        jdbc.update("DELETE FROM withdrawal_requests WHERE student_id IN (285001,285002)");
        jdbc.update("DELETE FROM academic_requests WHERE student_id IN (285001,285002)");
        jdbc.update("DELETE FROM students WHERE id IN (285001,285002)");
        jdbc.update("DELETE FROM professors WHERE id=285001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 285011 AND 285015");
        jdbc.update("DELETE FROM departments WHERE id=285001");
        jdbc.update("DELETE FROM colleges WHERE id=285001");
    }
}
