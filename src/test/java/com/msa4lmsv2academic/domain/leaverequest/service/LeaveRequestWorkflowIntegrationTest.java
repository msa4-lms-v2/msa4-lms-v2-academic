package com.msa4lmsv2academic.domain.leaverequest.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.leaverequest.entity.*;
import com.msa4lmsv2academic.domain.leaverequest.request.*;
import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;
import com.msa4lmsv2academic.domain.withdrawal.service.*;
import com.msa4lmsv2academic.domain.withdrawal.request.*;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"academic.leave.idempotency-cleanup.cron=-",
        "academic.enrollment.idempotency-cleanup.cron=-", "academic.withdrawal.idempotency-cleanup.cron=-"})
@AutoConfigureMockMvc
class LeaveRequestWorkflowIntegrationTest extends MySqlIntegrationTest {
    private static final CurrentUser STUDENT = new CurrentUser(280011L, "STUDENT");
    private static final CurrentUser OTHER = new CurrentUser(280012L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(280013L, "PROFESSOR");
    private static final CurrentUser ADMIN = new CurrentUser(280014L, "ADMIN");
    private static final LeaveAuditContext CONTEXT = new LeaveAuditContext("leave-test", "127.0.0.1");
    private static final WithdrawalAuditContext WD_CONTEXT = new WithdrawalAuditContext("leave-test", "127.0.0.1");
    private static final String URL = "/api/academic/leave-requests";
    @Autowired private LeaveRequestApplicationService application;
    @Autowired private LeaveRequestService service;
    @Autowired private LeavePeriodService periods;
    @Autowired private LeaveIdempotencyCleanupService cleanup;
    @Autowired private WithdrawalService withdrawals;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private FileStorageService storage;
    @MockitoSpyBean private AuditLogService audit;
    private List<Long> oldCurrentIds = List.of();

    @BeforeEach void setUp() {
        clean();
        oldCurrentIds = jdbc.queryForList("SELECT id FROM semesters WHERE is_current=1", Long.class);
        jdbc.update("UPDATE semesters SET is_current=0 WHERE is_current=1");
        jdbc.update("INSERT INTO colleges (id,code,name,active) VALUES (280001,'LV-COL','휴복학대학',1)");
        jdbc.update("INSERT INTO departments (id,code,college_id,name,active) VALUES (280001,'982',280001,'휴복학과',1)");
        jdbc.update("INSERT INTO users (id,name,role,status) VALUES (280011,'휴학학생','STUDENT','ACTIVE'),"
                + "(280012,'다른학생','STUDENT','ACTIVE'),(280013,'지도교수','PROFESSOR','ACTIVE'),(280014,'관리자','ADMIN','ACTIVE')");
        jdbc.update("INSERT INTO professors (id,version,user_id,hire_year,department_id) VALUES (280001,0,280013,2020,280001)");
        jdbc.update("INSERT INTO students (id,user_id,department_id,grade_level,admission_year,academic_status,advisor_id) VALUES "
                + "(280001,280011,280001,2,2089,'ENROLLED',280001),(280002,280012,280001,2,2089,'ENROLLED',280001)");
        semester(280001, 2090, "FIRST", true);
        semester(280002, 2090, "SECOND", false);
        semester(280003, 2092, "FIRST", false);
        for (var type : LeaveRequestType.values()) {
            long semesterId = type.isLeave() ? 280002L : 280003L;
            var now = LeaveRequestPolicy.now().withNano(0);
            jdbc.update("INSERT INTO leave_request_periods (semester_id,request_type,start_at,end_at,approval_start_at,approval_end_at,is_active)"
                    + " VALUES (?,?,?,?,?,?,1)", semesterId, type.name(), now.minusDays(1), now.plusDays(1),
                    now.minusDays(1), now.plusDays(1));
        }
        when(storage.uploadEvidence(anyString(), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "leave-requests/280011/test.pdf";
        });
        when(storage.download(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "%PDF-1.7 test".getBytes(StandardCharsets.UTF_8);
        });
    }

    @AfterEach void tearDown() {
        reset(auditTarget());
        clean();
        for (Long id : oldCurrentIds) jdbc.update("UPDATE semesters SET is_current=1 WHERE id=?", id);
    }

    @Test void generalLeaveCreateReplayCancelAndReapplyPreserveOriginalAndFullReasons() {
        var first = create("lv-create");
        assertThat(first.status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(first.returnYear()).isEqualTo((short) 2092);
        assertThat(application.create(general(), null, "lv-create", STUDENT, CONTEXT)).isEqualTo(first);
        assertThat(count("academic_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThatThrownBy(() -> create("lv-duplicate")).isInstanceOf(LeaveRequestConflictException.class);
        var cancelled = service.changeStatus(first.id(), new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED,
                "가".repeat(500)), "lv-cancel", STUDENT, CONTEXT);
        assertThat(cancelled.reason()).isEqualTo("개인 사정");
        assertThat(cancelled.cancelReason()).hasSize(500);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(jdbc.queryForObject("SELECT CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(after_value,'$.cancelReason'))) "
                + "FROM audit_logs WHERE actor_id=280011 AND action='LEAVE_CANCELLED'", Integer.class)).isEqualTo(500);
        assertThat(create("lv-reapply").id()).isNotEqualTo(first.id());
        verifyNoInteractions(storage);
    }

    @Test void approvalAndReturnUseRealHistoryAndExactPlannedSemester() {
        long id = create("lv-create").id();
        var result = approve(id, "lv-approve");
        assertThat(result.status()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(studentStatus()).isEqualTo("ON_LEAVE");
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThat(approve(id, "lv-approve")).isEqualTo(result);
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThatThrownBy(() -> application.create(returnBody((short) 2091, (byte) 2), null, "lv-early", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        // 클라이언트가 군복학을 보내도 실제 일반휴학 근거에서 일반복학으로 판별합니다.
        var returning = application.create(returnBody((short) 2092, (byte) 1), null, "lv-return", STUDENT, CONTEXT);
        assertThat(returning.requestType()).isEqualTo(LeaveRequestType.GENERAL_RETURN);
        assertThat(returning.reason()).isEqualTo("복학");
        approve(returning.id(), "lv-return-approve");
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(count("academic_status_histories")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM academic_status_histories WHERE student_id=280001 AND source_type='LEAVE_REQUEST'",
                Integer.class)).isEqualTo(2);
    }

    @Test void militaryUsesApplicationTermPlusFourAndDoesNotRecalculateOnApproval() {
        var first = application.create(military(), pdf(), "lv-military", STUDENT, CONTEXT);
        assertThat(first.returnYear()).isEqualTo((short) 2092);
        assertThat(first.returnSemester()).isEqualTo((byte) 1);
        assertThat(first.reason()).isEqualTo("군입대");
        assertThat(application.create(military(), pdf(), "lv-military", STUDENT, CONTEXT)).isEqualTo(first);
        verify(storage, times(1)).uploadEvidence(anyString(), any());
        jdbc.update("UPDATE semesters SET is_current=0 WHERE id=280001");
        jdbc.update("UPDATE semesters SET is_current=1 WHERE id=280002");
        var approved = approve(first.id(), "lv-military-approve");
        assertThat(approved.returnSemester()).isEqualTo((byte) 1);
        var returning = application.create(returnBody((short) 2092, (byte) 1), null, "lv-return", STUDENT, CONTEXT);
        assertThat(returning.requestType()).isEqualTo(LeaveRequestType.MILITARY_RETURN);
        approve(returning.id(), "lv-return-approve");
        assertThatThrownBy(() -> application.create(military(), pdf(), "lv-military-again", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        assertThat(jdbc.queryForObject("SELECT JSON_EXTRACT(after_value,'$.applicationSemesterId') FROM audit_logs "
                + "WHERE actor_id=280011 AND action='LEAVE_CREATED' AND target_id=?", String.class, first.id())).isEqualTo("280001");
    }

    @Test void missingCurrentTermAndUnprovenMigratedLeaveFailClosed() {
        jdbc.update("UPDATE semesters SET is_current=0 WHERE id=280001");
        assertThatThrownBy(() -> application.create(military(), pdf(), "lv-no-current", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        jdbc.update("UPDATE students SET academic_status='ON_LEAVE' WHERE id=280001");
        assertThatThrownBy(() -> application.create(returnBody((short) 2092, (byte) 1), null, "lv-no-origin", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        verifyNoInteractions(storage);
    }

    @Test void periodReceiptAndApprovalAreIndependentButCancelAndRejectIgnoreBoth() {
        jdbc.update("UPDATE leave_request_periods SET approval_start_at=?,approval_end_at=? WHERE request_type='GENERAL_LEAVE'",
                LeaveRequestPolicy.now().plusDays(2), LeaveRequestPolicy.now().plusDays(3));
        var first = create("lv-create");
        assertThatThrownBy(() -> approve(first.id(), "lv-no-approval-period")).isInstanceOf(LeaveRequestConflictException.class);
        jdbc.update("UPDATE leave_request_periods SET is_active=0");
        service.changeStatus(first.id(), new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.REJECTED, "보완 필요"),
                "lv-reject", ADMIN, CONTEXT);
        assertThatThrownBy(() -> create("lv-inactive")).isInstanceOf(LeaveRequestConflictException.class);
        jdbc.update("UPDATE leave_request_periods SET is_active=1");
        var second = create("lv-create2");
        jdbc.update("UPDATE leave_request_periods SET is_active=0");
        service.changeStatus(second.id(), new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED, "취소"),
                "lv-cancel", STUDENT, CONTEXT);
    }

    @Test void completedReceiptCanBeApprovedAfterReceiptCloses() {
        var first = create("lv-create");
        jdbc.update("UPDATE leave_request_periods SET start_at=?,end_at=? WHERE request_type='GENERAL_LEAVE'",
                LeaveRequestPolicy.now().minusDays(3), LeaveRequestPolicy.now().minusDays(2));
        assertThat(approve(first.id(), "lv-after-receipt").status()).isEqualTo(LeaveRequestStatus.APPROVED);
    }

    @Test void newApplicationNeedsExpectedStateAndApprovalRevalidatesIt() {
        var first = create("lv-create");
        jdbc.update("UPDATE students SET academic_status='WITHDRAWN' WHERE id=280001");
        assertThatThrownBy(() -> approve(first.id(), "lv-invalid-state")).isInstanceOf(LeaveRequestConflictException.class);
        assertThat(service.get(first.id(), STUDENT).status()).isEqualTo(LeaveRequestStatus.PENDING);
        // 취소는 학적 상태 변경 후에도 본인 대기 신청이면 가능합니다.
        service.changeStatus(first.id(), new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED, "취소"),
                "lv-cancel", STUDENT, CONTEXT);
        assertThatThrownBy(() -> create("lv-withdrawn")).isInstanceOf(LeaveRequestConflictException.class);
    }

    @Test void pdfValidationPrecedesUploadAndChangedFileCannotReplay() {
        assertThatThrownBy(() -> application.create(military(), null, "lv-no-pdf", STUDENT, CONTEXT))
                .isInstanceOf(InvalidFileException.class);
        var invalid = new MockMultipartFile("file", "fake.pdf", "application/pdf", "not pdf".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> application.create(military(), invalid, "lv-fake", STUDENT, CONTEXT))
                .isInstanceOf(InvalidFileException.class);
        verifyNoInteractions(storage);
        application.create(military(), pdf(), "lv-file", STUDENT, CONTEXT);
        var changed = new MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.7 other".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> application.create(military(), changed, "lv-file", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        verify(storage, times(1)).uploadEvidence(anyString(), any());
    }

    @Test void failedApprovalAuditRollsBackStateHistoryAndKey() {
        long id = create("lv-create").id();
        doThrow(new IllegalStateException("forced")).when(auditTarget()).record(anyLong(), eq("LEAVE_APPROVED"),
                anyString(), anyLong(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> approve(id, "lv-rollback")).isInstanceOf(IllegalStateException.class);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(service.get(id, STUDENT).status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(count("academic_status_histories")).isZero();
        assertThat(count("idempotency_keys")).isEqualTo(1);
    }

    @Test void withdrawalFinalApprovalAutoCancelsPendingAndReplayDoesNotRepeatAudit() {
        var leave = create("lv-create");
        long wd = advisorApprovedWithdrawal();
        assertThat(service.get(leave.id(), STUDENT).status()).isEqualTo(LeaveRequestStatus.PENDING);
        var approved = finalApprove(wd, "lv-wd-final");
        assertThat(studentStatus()).isEqualTo("WITHDRAWN");
        assertThat(service.get(leave.id(), STUDENT).status()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(count("academic_status_histories")).isEqualTo(1);
        assertThat(finalApprove(wd, "lv-wd-final")).isEqualTo(approved);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=280014 AND action='LEAVE_WITHDRAWAL_CANCELLED'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT JSON_EXTRACT(after_value,'$.withdrawalId') FROM audit_logs "
                + "WHERE actor_id=280014 AND action='LEAVE_WITHDRAWAL_CANCELLED'", String.class)).isEqualTo(Long.toString(wd));
        assertThatThrownBy(() -> approve(leave.id(), "lv-too-late")).isInstanceOf(LeaveRequestConflictException.class);
    }

    @Test void cancellationAuditFailureRollsBackWholeWithdrawalApproval() {
        var leave = create("lv-create");
        long wd = advisorApprovedWithdrawal();
        doThrow(new IllegalStateException("forced auto-cancel audit")).when(auditTarget()).record(anyLong(),
                eq("LEAVE_WITHDRAWAL_CANCELLED"), anyString(), anyLong(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> finalApprove(wd, "lv-wd-rollback")).isInstanceOf(RuntimeException.class);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(service.get(leave.id(), STUDENT).status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(withdrawals.get(wd, STUDENT).status().name()).isEqualTo("ADVISOR_APPROVED");
        assertThat(count("academic_status_histories")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_keys WHERE idempotency_key='lv-wd-rollback'", Integer.class)).isZero();
    }

    @Test void withdrawalKeepsApprovedLeaveAndItsFile() {
        var leave = application.create(military(), pdf(), "lv-file", STUDENT, CONTEXT);
        approve(leave.id(), "lv-approve");
        finalApprove(advisorApprovedWithdrawal(), "lv-final");
        assertThat(service.get(leave.id(), STUDENT).status()).isEqualTo(LeaveRequestStatus.APPROVED);
        assertThat(application.download(leave.id(), ADMIN).bytes()).startsWith("%PDF".getBytes(StandardCharsets.UTF_8));
        verify(storage, never()).delete(anyString());
        assertThat(count("academic_status_histories")).isEqualTo(2);
    }

    @Test void concurrentCreateOnlyOnePendingAndDuplicateKeysReplay() throws Exception {
        var outcomes = race(() -> attemptCreate("lv-concurrent-a"), () -> attemptCreate("lv-concurrent-b"));
        assertThat(outcomes).containsExactlyInAnyOrder("PENDING", "CONFLICT");
        assertThat(count("academic_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test void simultaneousSameKeyCreatesOneRequestAndOneAudit() throws Exception {
        var outcomes = race(() -> attemptCreate("lv-same-key"), () -> attemptCreate("lv-same-key"));
        assertThat(outcomes).containsExactly("PENDING", "PENDING");
        assertThat(count("academic_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test void approvalAndCancellationCannotBothSucceed() throws Exception {
        long id = create("lv-create").id();
        var outcomes = race(() -> outcome(() -> approve(id, "lv-approve")),
                () -> outcome(() -> service.changeStatus(id,
                        new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED, "취소"), "lv-cancel", STUDENT, CONTEXT)));
        assertThat(outcomes).contains("CONFLICT");
        assertThat(outcomes.stream().filter("CONFLICT"::equals)).hasSize(1);
        var status = service.get(id, STUDENT).status();
        assertThat(studentStatus()).isEqualTo(status == LeaveRequestStatus.APPROVED ? "ON_LEAVE" : "ENROLLED");
        assertThat(count("academic_status_histories")).isEqualTo(status == LeaveRequestStatus.APPROVED ? 1 : 0);
    }

    @Test void withdrawalAndLeaveApprovalRaceKeepsConsistentFinalState() throws Exception {
        long leaveId = create("lv-create").id();
        long wd = advisorApprovedWithdrawal();
        race(() -> outcome(() -> approve(leaveId, "lv-race-approve")), () -> {
            finalApprove(wd, "lv-race-final");
            return "WITHDRAWN";
        });
        assertThat(studentStatus()).isEqualTo("WITHDRAWN");
        var status = service.get(leaveId, STUDENT).status();
        assertThat(status).isIn(LeaveRequestStatus.APPROVED, LeaveRequestStatus.CANCELLED);
        assertThat(count("academic_status_histories")).isEqualTo(status == LeaveRequestStatus.APPROVED ? 2 : 1);
    }

    @Test void periodUniquenessInactiveReuseAndAuditReplay() {
        var now = LeaveRequestPolicy.now().withNano(0);
        var body = new LeavePeriodSaveRequestDTO(280001L, LeaveRequestType.GENERAL_RETURN, now.minusDays(1),
                now.plusDays(1), now, now.plusDays(2), true, "기간 생성");
        var first = periods.create(body, "lv-period-create", ADMIN, CONTEXT);
        assertThat(periods.create(body, "lv-period-create", ADMIN, CONTEXT)).isEqualTo(first);
        var off = new LeavePeriodSaveRequestDTO(280001L, body.requestType(), body.startAt(), body.endAt(),
                body.approvalStartAt(), body.approvalEndAt(), false, "기간 중지");
        var changed = periods.update(first.id(), off, "lv-period-off", ADMIN, CONTEXT);
        assertThat(changed.active()).isFalse();
        assertThat(periods.update(first.id(), off, "lv-period-off", ADMIN, CONTEXT)).isEqualTo(changed);
        assertThatThrownBy(() -> periods.create(body, "lv-period-duplicate", ADMIN, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        var studentPeriods = periods.search(new LeavePeriodSearchRequestDTO(1,20,280001L,null,null), STUDENT, PageRequest.of(0,20));
        assertThat(studentPeriods.items()).isEmpty();
        assertThatThrownBy(() -> periods.create(body,"lv-period-student",STUDENT,CONTEXT))
                .isInstanceOf(LeaveRequestAccessDeniedException.class);
    }

    @Test void completedReplayAfterCancellationDoesNotRepresentCurrentStateAndExpiryChecksNewRules() {
        var first = create("lv-create");
        service.changeStatus(first.id(),new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED,"취소"),
                "lv-cancel",STUDENT,CONTEXT);
        assertThat(create("lv-create")).isEqualTo(first);
        assertThat(service.get(first.id(),STUDENT).status()).isEqualTo(LeaveRequestStatus.CANCELLED);
        jdbc.update("UPDATE idempotency_keys SET expires_at=? WHERE idempotency_key='lv-create'", LeaveRequestPolicy.now().minusSeconds(1));
        assertThat(create("lv-create").id()).isNotEqualTo(first.id());
        assertThat(count("academic_requests")).isEqualTo(2);
    }

    @Test void cleanupOnlyRemovesExpiredCompletedLeaveKeys() {
        create("lv-create");
        jdbc.update("UPDATE idempotency_keys SET expires_at=? WHERE idempotency_key='lv-create'", LeaveRequestPolicy.now().minusSeconds(1));
        withdrawals.create(new WithdrawalCreateRequestDTO("자퇴",null),"lv-other-endpoint",STUDENT,WD_CONTEXT);
        jdbc.update("UPDATE idempotency_keys SET expires_at=? WHERE idempotency_key='lv-other-endpoint'", LeaveRequestPolicy.now().minusSeconds(1));
        cleanup.removeExpiredCompletedKeys();
        assertThat(count("idempotency_keys")).isEqualTo(1);
        assertThat(count("academic_requests")).isEqualTo(1);
        assertThat(count("audit_logs")).isEqualTo(2);
    }

    @Test void httpRolesOwnershipFiltersAndMultipartContract() throws Exception {
        var part = new MockMultipartFile("request","request.json","application/json",mapper.writeValueAsBytes(general()));
        var result = mvc.perform(multipart(URL).file(part).header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT")
                .header("Idempotency-Key","lv-http")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING")).andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(get(URL+"/"+id).header("X-User-Id",OTHER.id()).header("X-User-Role","STUDENT"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E03"));
        mvc.perform(get(URL).header("X-User-Id",PROFESSOR.id()).header("X-User-Role","PROFESSOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header("X-User-Id",ADMIN.id()).header("X-User-Role","ADMIN").param("status","REJECTED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty()).andExpect(jsonPath("$.data.totalCount").value(0));
        mvc.perform(get(URL).header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT").param("studentId","280002"))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT").param("size","101"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(URL+"/"+id+"/status").header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT")
                .header("Idempotency-Key","lv-forbidden").contentType("application/json").content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void attachmentIsAuthorizedAndStoredKeyNeverAppearsInResponse() throws Exception {
        long id = application.create(military(),pdf(),"lv-file",STUDENT,CONTEXT).id();
        mvc.perform(get(URL+"/"+id).header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.attachmentStoredName").doesNotExist());
        mvc.perform(get(URL+"/"+id+"/attachment").header("X-User-Id",OTHER.id()).header("X-User-Role","STUDENT"))
                .andExpect(status().isForbidden());
        verify(storage,never()).download(anyString());
        mvc.perform(get(URL+"/"+id+"/attachment").header("X-User-Id",STUDENT.id()).header("X-User-Role","STUDENT"))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
    }

    @Test void openApiContainsAllOperationsSchemasSecurityAndPreservesWithdrawal201() throws Exception {
        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/leave-requests']['post']['operationId']").value("createLeaveRequest"))
                .andExpect(jsonPath("$['paths']['/api/academic/leave-requests']['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['paths']['/api/academic/leave-requests']['post']['requestBody']['content']['multipart/form-data']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/leave-requests/{id}/status']['patch']['responses']['409']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/leave-requests/{id}/attachment']['get']['responses']['200']['content']['application/pdf']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/leave-request-periods/{id}']['put']['operationId']").value("updateLeaveRequestPeriod"))
                .andExpect(jsonPath("$['paths']['/api/academic/withdrawals']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['components']['schemas']['LeaveRequestCreateRequestDTO']['properties']['returnYear']").exists())
                .andExpect(jsonPath("$['components']['schemas']['LeavePeriodSaveRequestDTO']['properties']['approvalStartAt']").exists());
    }

    @ParameterizedTest
    @EnumSource(LeaveRequestType.class)
    void finalWithdrawalCancelsAllFourPendingTypes(LeaveRequestType type) {
        if (!type.isLeave()) jdbc.update("UPDATE students SET academic_status='ON_LEAVE' WHERE id=280001");
        jdbc.update("INSERT INTO academic_requests (student_id,request_type,reason,target_year,target_semester,status) "
                + "VALUES (280001,?,'기존 대기 신청',2090,2,'PENDING')", type.name());
        long id = jdbc.queryForObject("SELECT id FROM academic_requests WHERE student_id=280001", Long.class);
        finalApprove(advisorApprovedWithdrawal(), "lv-auto-all");
        assertThat(service.get(id, STUDENT).status()).isEqualTo(LeaveRequestStatus.CANCELLED);
        assertThat(service.get(id, STUDENT).cancelReason()).isEqualTo("자퇴 최종 승인으로 자동 취소되었습니다.");
        assertThat(studentStatus()).isEqualTo("WITHDRAWN");
    }

    @Test void finalWithdrawalAuditFailureAlsoRollsBackSuccessfulAutoCancellation() {
        long leaveId = create("lv-create").id();
        long wd = advisorApprovedWithdrawal();
        doThrow(new IllegalStateException("forced final audit")).when(auditTarget()).record(anyLong(),
                eq("WITHDRAWAL_APPROVED"), anyString(), anyLong(), any(), any(), any(), any(), any());
        assertThatThrownBy(() -> finalApprove(wd, "lv-final-audit-fail")).isInstanceOf(RuntimeException.class);
        assertThat(service.get(leaveId, STUDENT).status()).isEqualTo(LeaveRequestStatus.PENDING);
        assertThat(studentStatus()).isEqualTo("ENROLLED");
        assertThat(count("academic_status_histories")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=280014 AND action='LEAVE_WITHDRAWAL_CANCELLED'",
                Integer.class)).isZero();
    }

    @Test void approvalReasonIsPreservedInAuditAndNotMistakenForOriginalRequestReason() {
        long id = create("lv-create").id();
        var result = service.changeStatus(id, new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.APPROVED, "승".repeat(500)),
                "lv-approval-reason", ADMIN, CONTEXT);
        assertThat(result.reason()).isEqualTo("개인 사정");
        assertThat(jdbc.queryForObject("SELECT CHAR_LENGTH(JSON_UNQUOTE(JSON_EXTRACT(after_value,'$.decisionReason'))) "
                + "FROM audit_logs WHERE actor_id=280014 AND action='LEAVE_APPROVED'", Integer.class)).isEqualTo(500);
    }

    @Test void missingPeriodBlocksWithoutUploadingAndMigrationKeepsExistingRows() {
        var first = create("lv-create");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("migration/20260828_create_leave_requests_and_periods.sql"))
                .execute(jdbc.getDataSource());
        assertThat(service.get(first.id(), STUDENT).reason()).isEqualTo(first.reason());
        service.changeStatus(first.id(), new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.CANCELLED,"취소"),
                "lv-cancel",STUDENT,CONTEXT);
        jdbc.update("DELETE FROM leave_request_periods WHERE semester_id=280002");
        assertThatThrownBy(() -> application.create(military(), pdf(), "lv-no-period", STUDENT, CONTEXT))
                .isInstanceOf(LeaveRequestConflictException.class);
        verifyNoInteractions(storage);
    }

    private LeaveRequestCreateRequestDTO general() {
        return new LeaveRequestCreateRequestDTO(LeaveRequestType.GENERAL_LEAVE,"  개인 사정  ",(short)2090,(byte)2,(short)2092,(byte)1);
    }
    private LeaveRequestCreateRequestDTO military() {
        return new LeaveRequestCreateRequestDTO(LeaveRequestType.MILITARY_LEAVE,null,(short)2090,(byte)2,null,null);
    }
    private LeaveRequestCreateRequestDTO returnBody(short year,byte term) {
        return new LeaveRequestCreateRequestDTO(LeaveRequestType.MILITARY_RETURN,null,year,term,null,null);
    }
    private MockMultipartFile pdf() {
        return new MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.7 test".getBytes(StandardCharsets.UTF_8));
    }
    private LeaveRequestResponseDTO create(String key) {
        return application.create(general(),null,key,STUDENT,CONTEXT);
    }
    private LeaveRequestResponseDTO approve(long id,String key) {
        return service.changeStatus(id,new LeaveRequestStatusChangeRequestDTO(LeaveRequestStatus.APPROVED,null),key,ADMIN,CONTEXT);
    }
    private long advisorApprovedWithdrawal() {
        var wd = withdrawals.create(new WithdrawalCreateRequestDTO("자퇴",null),"lv-wd-create",STUDENT,WD_CONTEXT);
        withdrawals.reviewByAdvisor(wd.id(),new AdvisorWithdrawalReviewRequestDTO(true,null),"lv-wd-advisor",PROFESSOR,WD_CONTEXT);
        return wd.id();
    }
    private com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO finalApprove(long id,String key) {
        return withdrawals.reviewByAdmin(id,new FinalWithdrawalReviewRequestDTO(true,LocalDate.now(java.time.ZoneId.of("Asia/Seoul")),null),
                key,ADMIN,WD_CONTEXT);
    }
    private String studentStatus() {
        return jdbc.queryForObject("SELECT academic_status FROM students WHERE id=280001",String.class);
    }
    private int count(String table) {
        String column = switch(table) { case "audit_logs" -> "actor_id"; case "idempotency_keys" -> "requester_user_id"; default -> "student_id"; };
        return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+column+" BETWEEN 280001 AND 280014",Integer.class);
    }
    private AuditLogService auditTarget() { return AopTestUtils.getUltimateTargetObject(audit); }
    private String attemptCreate(String key) throws Exception { return outcome(() -> create(key)); }
    private String outcome(Callable<LeaveRequestResponseDTO> operation) throws Exception {
        try { return operation.call().status().name(); } catch(LeaveRequestConflictException failure) { return "CONFLICT"; }
    }
    private <T> List<T> race(Callable<T> a,Callable<T> b) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try(ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { ready.countDown(); start.await(); return a.call(); });
            var second = pool.submit(() -> { ready.countDown(); start.await(); return b.call(); });
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
        }
    }
    private void semester(long id,int year,String term,boolean current) {
        jdbc.update("INSERT INTO semesters (id,academic_year,term,start_date,end_date,enrollment_start_at,enrollment_end_at,is_current) "
                + "VALUES (?,?,?,'2090-03-01','2090-06-30','2090-02-01 09:00:00','2090-02-10 18:00:00',?)",id,year,term,current);
    }
    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 280011 AND 280014");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 280011 AND 280014");
        jdbc.update("DELETE FROM academic_status_histories WHERE student_id IN (280001,280002)");
        jdbc.update("DELETE FROM withdrawal_requests WHERE student_id IN (280001,280002)");
        jdbc.update("DELETE FROM academic_requests WHERE student_id IN (280001,280002)");
        jdbc.update("DELETE FROM leave_request_periods WHERE semester_id BETWEEN 280001 AND 280003");
        jdbc.update("DELETE FROM students WHERE id IN (280001,280002)");
        jdbc.update("DELETE FROM professors WHERE id=280001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 280011 AND 280014");
        jdbc.update("DELETE FROM departments WHERE id=280001");
        jdbc.update("DELETE FROM colleges WHERE id=280001");
        jdbc.update("DELETE FROM semesters WHERE id BETWEEN 280001 AND 280003");
    }
}
