package com.msa4lmsv2academic.domain.transfer.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.request.*;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"academic.leave.idempotency-cleanup.cron=-",
        "academic.enrollment.idempotency-cleanup.cron=-", "academic.withdrawal.idempotency-cleanup.cron=-"})
@AutoConfigureMockMvc
class DepartmentTransferWorkflowIntegrationTest extends MySqlIntegrationTest {
    private static final CurrentUser STUDENT = new CurrentUser(295011L, "STUDENT");
    private static final CurrentUser OTHER = new CurrentUser(295012L, "STUDENT");
    private static final CurrentUser ADMIN = new CurrentUser(295013L, "ADMIN");
    private static final CurrentUser PROFESSOR = new CurrentUser(295014L, "PROFESSOR");
    private static final DepartmentTransferAuditContext CONTEXT =
            new DepartmentTransferAuditContext("transfer-test", "127.0.0.1");

    @Autowired private DepartmentTransferApplicationService application;
    @Autowired private DepartmentTransferService service;
    @Autowired private DepartmentTransferPeriodService periodService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @MockitoBean private FileStorageService storage;

    @BeforeEach
    void setUp() {
        clean();
        jdbc.update("INSERT INTO colleges (id,code,name,active) VALUES (295001,'TR-COL','전과대학',1)");
        jdbc.update("INSERT INTO departments (id,code,college_id,name,active) VALUES "
                + "(295001,'951',295001,'출발학과',1),(295002,'952',295001,'희망학과',1),(295003,'953',295001,'복수전공학과',1)");
        jdbc.update("INSERT INTO majors (id,department_id,code,name,active) VALUES "
                + "(295001,295001,'TR-SRC','출발전공',1),(295002,295002,'TR-DST','희망전공',1),"
                + "(295003,295003,'TR-DBL','복수전공',1)");
        jdbc.update("INSERT INTO users (id,name,role,status) VALUES "
                + "(295011,'전과학생','STUDENT','ACTIVE'),(295012,'다른학생','STUDENT','ACTIVE'),"
                + "(295013,'관리자','ADMIN','ACTIVE'),(295014,'기존지도교수','PROFESSOR','ACTIVE')");
        jdbc.update("INSERT INTO professors (id,version,user_id,hire_year,department_id) VALUES (295001,0,295014,2020,295001)");
        jdbc.update("INSERT INTO students (id,user_id,department_id,major_id,double_major_id,grade_level,admission_year,academic_status,advisor_id) VALUES "
                + "(295001,295011,295001,295001,295003,2,2025,'ENROLLED',295001),"
                + "(295002,295012,295001,295001,NULL,2,2025,'ENROLLED',295001)");
        jdbc.update("INSERT INTO semesters (id,academic_year,term,start_date,end_date,enrollment_start_at,enrollment_end_at,is_current) "
                + "VALUES (295001,2027,'FIRST','2027-03-02','2027-06-18','2027-02-10 09:00:00','2027-02-14 18:00:00',0)");
        LocalDateTime now = DepartmentTransferPolicy.now().withNano(0);
        jdbc.update("INSERT INTO academic_change_request_periods "
                        + "(semester_id,request_type,start_at,end_at,is_active) VALUES (295001,'TRANSFER_DEPARTMENT',?,?,1)",
                now.minusDays(1), now.plusDays(1));
        AtomicInteger sequence = new AtomicInteger();
        when(storage.uploadEvidence(anyString(), any())).thenAnswer(invocation ->
                "department-transfer-requests/test/" + sequence.incrementAndGet() + ".pdf");
        when(storage.download(anyString())).thenReturn("%PDF-1.7 test".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void createReplaysSameThreeFilesAndExposesOnlyMetadata() {
        var created = create("transfer-create");
        assertThat(created.status()).isEqualTo(AcademicChangeRequestStatus.PENDING);
        assertThat(created.sourceDepartmentName()).isEqualTo("출발학과");
        assertThat(created.targetDepartmentName()).isEqualTo("희망학과");
        assertThat(created.documents()).extracting(item -> item.documentType())
                .containsExactly(TransferDocumentType.SELF_INTRODUCTION, TransferDocumentType.STUDY_PLAN,
                        TransferDocumentType.TRANSCRIPT);
        assertThat(create("transfer-create")).isEqualTo(created);
        verify(storage, times(3)).uploadEvidence(anyString(), any());
        assertThat(count("academic_change_requests", "student_id")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM academic_change_request_files WHERE request_id=?",
                Integer.class, created.id())).isEqualTo(3);
        var detail = service.get(created.id(), STUDENT);
        assertThat(detail.id()).isEqualTo(created.id());
        assertThat(detail.status()).isEqualTo(created.status());
        assertThat(detail.documents()).extracting(item -> item.documentType())
                .containsExactly(TransferDocumentType.SELF_INTRODUCTION, TransferDocumentType.STUDY_PLAN,
                        TransferDocumentType.TRANSCRIPT);
        assertThatThrownBy(() -> service.get(created.id(), OTHER))
                .isInstanceOf(DepartmentTransferAccessDeniedException.class);
        var download = application.download(created.id(), TransferDocumentType.TRANSCRIPT, STUDENT);
        assertThat(download.originalName()).isEqualTo("성적증명서.pdf");
    }

    @Test
    void allThreeValidPdfFilesAreRequiredBeforeRemoteUpload() {
        assertThatThrownBy(() -> application.create(body(), pdf("자기소개서.pdf"), pdf("학업계획서.pdf"), null,
                "transfer-missing", STUDENT, CONTEXT)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void cancellationPreservesOriginalAndFilesAndAllowsReapplication() {
        var first = create("transfer-create");
        var cancelled = service.cancel(first.id(), new DepartmentTransferCancelRequestDTO("진로 재검토"),
                "transfer-cancel", STUDENT, CONTEXT);
        assertThat(cancelled.status()).isEqualTo(AcademicChangeRequestStatus.CANCELLED);
        assertThat(cancelled.documents()).hasSize(3);
        assertThat(create("transfer-reapply").id()).isNotEqualTo(first.id());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void approvalChangesAffiliationImmediatelyClearsAdvisorAndKeepsDoubleMajor() {
        var created = create("transfer-create");
        var approved = service.review(created.id(),
                new DepartmentTransferReviewRequestDTO(AcademicChangeRequestStatus.APPROVED, null),
                "transfer-approve", ADMIN, CONTEXT);
        assertThat(approved.status()).isEqualTo(AcademicChangeRequestStatus.APPROVED);
        var row = jdbc.queryForMap("SELECT department_id,major_id,double_major_id,advisor_id FROM students WHERE id=295001");
        assertThat(((Number) row.get("department_id")).longValue()).isEqualTo(295002L);
        assertThat(((Number) row.get("major_id")).longValue()).isEqualTo(295002L);
        assertThat(((Number) row.get("double_major_id")).longValue()).isEqualTo(295003L);
        assertThat(row.get("advisor_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=295013 "
                + "AND action IN ('TRANSFER_REQUEST_APPROVED','STUDENT_TRANSFER_APPLIED')", Integer.class)).isEqualTo(2);
    }

    @Test
    void staleSourceAffiliationAndDoubleMajorCollisionFailClosed() {
        var created = create("transfer-create");
        jdbc.update("UPDATE students SET department_id=295003,major_id=295003,double_major_id=NULL WHERE id=295001");
        assertThatThrownBy(() -> service.review(created.id(),
                new DepartmentTransferReviewRequestDTO(AcademicChangeRequestStatus.APPROVED, null),
                "transfer-stale", ADMIN, CONTEXT)).isInstanceOf(DepartmentTransferConflictException.class);
        jdbc.update("UPDATE students SET department_id=295001,major_id=295001,double_major_id=295002 WHERE id=295001");
        assertThatThrownBy(() -> application.create(body(), pdf("자기소개서.pdf"), pdf("학업계획서.pdf"),
                pdf("성적증명서.pdf"), "transfer-double-major", STUDENT, CONTEXT))
                .isInstanceOf(DepartmentTransferConflictException.class);
    }

    @Test
    void rejectionKeepsStudentAffiliationAndRequiresReason() {
        var created = create("transfer-create");
        var rejected = service.review(created.id(),
                new DepartmentTransferReviewRequestDTO(AcademicChangeRequestStatus.REJECTED, "모집 기준 미충족"),
                "transfer-reject", ADMIN, CONTEXT);
        assertThat(rejected.status()).isEqualTo(AcademicChangeRequestStatus.REJECTED);
        assertThat(rejected.rejectReason()).isEqualTo("모집 기준 미충족");
        assertThat(jdbc.queryForObject("SELECT department_id FROM students WHERE id=295001", Long.class))
                .isEqualTo(295001L);
    }

    @Test
    void periodIsUniquePerSemesterAndAdminCanDisableIt() {
        var now = DepartmentTransferPolicy.now().withNano(0);
        var duplicate = new DepartmentTransferPeriodSaveRequestDTO(295001L, now.minusHours(1), now.plusHours(1),
                true, "중복 등록");
        assertThatThrownBy(() -> periodService.create(duplicate, "transfer-period-duplicate", ADMIN, CONTEXT))
                .isInstanceOf(DepartmentTransferConflictException.class);
        Long periodId = jdbc.queryForObject("SELECT id FROM academic_change_request_periods WHERE semester_id=295001", Long.class);
        var disabled = periodService.changeStatus(periodId,
                new DepartmentTransferPeriodStatusRequestDTO(false, "모집 종료"),
                "transfer-period-close", ADMIN, CONTEXT);
        assertThat(disabled.active()).isFalse();
        assertThatThrownBy(() -> create("transfer-closed")).isInstanceOf(DepartmentTransferConflictException.class);
    }

    @Test
    void roleScopeListAndOpenApiContractsAreExposed() throws Exception {
        create("transfer-create");
        PageResponseDTO<?> own = service.search(new DepartmentTransferSearchRequestDTO(null, null, null, null,
                null, null, null, null), STUDENT, PageRequest.of(0, 20));
        assertThat(own.totalCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.search(new DepartmentTransferSearchRequestDTO(null, null, null, null,
                null, null, null, null), PROFESSOR, PageRequest.of(0, 20)))
                .isInstanceOf(DepartmentTransferAccessDeniedException.class);

        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/department-transfer-requests']['post']['operationId']")
                        .value("createDepartmentTransferRequest"))
                .andExpect(jsonPath("$['paths']['/api/academic/department-transfer-requests']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/department-transfer-requests/{requestId}/review']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/department-transfer-requests/{requestId}/documents/{documentType}']['get']['responses']['200']['content']['application/pdf']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/catalog/department-transfer-periods/{periodId}/status']['patch']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DepartmentTransferCreateRequestDTO']['required']").isArray())
                .andExpect(jsonPath("$['components']['schemas']['DepartmentTransferResponseDTO']['properties']['documents']").exists());
    }

    private DepartmentTransferResponseDTO create(String key) {
        return application.create(body(), pdf("자기소개서.pdf"), pdf("학업계획서.pdf"), pdf("성적증명서.pdf"),
                key, STUDENT, CONTEXT);
    }

    private DepartmentTransferCreateRequestDTO body() {
        return new DepartmentTransferCreateRequestDTO(295002L, 295002L, 295001L, "진로 변경");
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf",
                "%PDF-1.7 transfer".getBytes(StandardCharsets.UTF_8));
    }

    private int count(String table, String column) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " BETWEEN 295001 AND 295020",
                Integer.class);
    }

    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 295011 AND 295014");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 295011 AND 295014");
        jdbc.update("DELETE FROM academic_change_request_files WHERE request_id IN "
                + "(SELECT id FROM academic_change_requests WHERE student_id IN (295001,295002))");
        jdbc.update("DELETE FROM academic_change_requests WHERE student_id IN (295001,295002)");
        jdbc.update("DELETE FROM academic_change_request_periods WHERE semester_id=295001");
        jdbc.update("DELETE FROM students WHERE id IN (295001,295002)");
        jdbc.update("DELETE FROM professors WHERE id=295001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 295011 AND 295014");
        jdbc.update("DELETE FROM majors WHERE id BETWEEN 295001 AND 295003");
        jdbc.update("DELETE FROM departments WHERE id BETWEEN 295001 AND 295003");
        jdbc.update("DELETE FROM colleges WHERE id=295001");
        jdbc.update("DELETE FROM semesters WHERE id=295001");
    }
}
