package com.msa4lmsv2academic.domain.doublemajor.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.service.DepartmentTransferAuditContext;
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
class DoubleMajorWorkflowIntegrationTest extends MySqlIntegrationTest {
    private static final CurrentUser STUDENT = new CurrentUser(296011L, "STUDENT");
    private static final CurrentUser OTHER = new CurrentUser(296012L, "STUDENT");
    private static final CurrentUser ADMIN = new CurrentUser(296013L, "ADMIN");
    private static final CurrentUser PROFESSOR = new CurrentUser(296014L, "PROFESSOR");
    private static final DepartmentTransferAuditContext CONTEXT =
            new DepartmentTransferAuditContext("double-major-test", "127.0.0.1");

    @Autowired private DoubleMajorApplicationService application;
    @Autowired private DoubleMajorService service;
    @Autowired private DoubleMajorPeriodService periodService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @MockitoBean private FileStorageService storage;

    @BeforeEach
    void setUp() {
        clean();
        jdbc.update("INSERT INTO colleges (id,code,name,active) VALUES (296001,'DM-COL','복수전공대학',1)");
        jdbc.update("INSERT INTO departments (id,code,college_id,name,active) VALUES "
                + "(296001,'961',296001,'주전공학과',1),(296002,'962',296001,'복수전공학과',1)");
        jdbc.update("INSERT INTO majors (id,department_id,code,name,active) VALUES "
                + "(296001,296001,'DM-MAIN','주전공',1),(296002,296002,'DM-TARGET','희망복수전공',1),"
                + "(296003,296002,'DM-EXIST','기존복수전공',1)");
        jdbc.update("INSERT INTO users (id,name,role,status) VALUES "
                + "(296011,'복수전공학생','STUDENT','ACTIVE'),(296012,'다른학생','STUDENT','ACTIVE'),"
                + "(296013,'관리자','ADMIN','ACTIVE'),(296014,'지도교수','PROFESSOR','ACTIVE')");
        jdbc.update("INSERT INTO professors (id,version,user_id,hire_year,department_id) VALUES (296001,0,296014,2020,296001)");
        jdbc.update("INSERT INTO students (id,user_id,department_id,major_id,double_major_id,grade_level,admission_year,academic_status,advisor_id) VALUES "
                + "(296001,296011,296001,296001,NULL,2,2025,'ENROLLED',296001),"
                + "(296002,296012,296001,296001,NULL,2,2025,'ENROLLED',296001)");
        jdbc.update("INSERT INTO semesters (id,academic_year,term,start_date,end_date,enrollment_start_at,enrollment_end_at,is_current) "
                + "VALUES (296001,2027,'FIRST','2027-03-02','2027-06-18','2027-02-10 09:00:00','2027-02-14 18:00:00',0),"
                + "(296002,2027,'SECOND','2027-09-01','2027-12-17','2027-08-10 09:00:00','2027-08-14 18:00:00',0)");
        LocalDateTime now = DoubleMajorPolicy.now().withNano(0);
        jdbc.update("INSERT INTO academic_change_request_periods "
                        + "(semester_id,request_type,start_at,end_at,is_active) VALUES (296001,'DOUBLE_MAJOR',?,?,1)",
                now.minusDays(1), now.plusDays(1));
        AtomicInteger sequence = new AtomicInteger();
        when(storage.uploadEvidence(anyString(), any())).thenAnswer(invocation ->
                "double-major-requests/test/" + sequence.incrementAndGet() + ".pdf");
        when(storage.download(anyString())).thenReturn("%PDF-1.7 test".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void createUsesOpenRecruitmentPeriodWithoutTargetSemesterAndReplays() {
        var created = create("double-major-create");
        assertThat(created.status()).isEqualTo(AcademicChangeRequestStatus.PENDING);
        assertThat(created.targetMajorName()).isEqualTo("희망복수전공");
        assertThat(created.requestPeriodId()).isNotNull();
        assertThat(created.documents()).extracting(item -> item.documentType())
                .containsExactly(TransferDocumentType.SELF_INTRODUCTION, TransferDocumentType.STUDY_PLAN,
                        TransferDocumentType.TRANSCRIPT);
        assertThat(create("double-major-create")).isEqualTo(created);
        verify(storage, times(3)).uploadEvidence(anyString(), any());
        var row = jdbc.queryForMap("SELECT target_semester_id,request_period_id FROM academic_change_requests WHERE id=?",
                created.id());
        assertThat(row.get("target_semester_id")).isNull();
        assertThat(row.get("request_period_id")).isNotNull();
        assertThatThrownBy(() -> service.get(created.id(), OTHER)).isInstanceOf(DoubleMajorAccessDeniedException.class);
        assertThat(application.download(created.id(), TransferDocumentType.TRANSCRIPT, STUDENT).originalName())
                .isEqualTo("성적증명서.pdf");
    }

    @Test
    void allThreePdfFilesAreRequiredBeforeRemoteUpload() {
        assertThatThrownBy(() -> application.create(body(), pdf("자기소개서.pdf"), pdf("학업계획서.pdf"), null,
                "double-major-missing", STUDENT, CONTEXT)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void cancellationPreservesFilesAndAllowsReapplication() {
        var first = create("double-major-create");
        var cancelled = service.cancel(first.id(), new DoubleMajorCancelRequestDTO("진로 재검토"),
                "double-major-cancel", STUDENT, CONTEXT);
        assertThat(cancelled.status()).isEqualTo(AcademicChangeRequestStatus.CANCELLED);
        assertThat(cancelled.documents()).hasSize(3);
        assertThat(create("double-major-reapply").id()).isNotEqualTo(first.id());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void approvalAssignsOnlyDoubleMajorImmediatelyAndWritesAudit() {
        var created = create("double-major-create");
        var approved = service.review(created.id(),
                new DoubleMajorReviewRequestDTO(AcademicChangeRequestStatus.APPROVED, null),
                "double-major-approve", ADMIN, CONTEXT);
        assertThat(approved.status()).isEqualTo(AcademicChangeRequestStatus.APPROVED);
        var row = jdbc.queryForMap("SELECT department_id,major_id,double_major_id,advisor_id FROM students WHERE id=296001");
        assertThat(((Number) row.get("department_id")).longValue()).isEqualTo(296001L);
        assertThat(((Number) row.get("major_id")).longValue()).isEqualTo(296001L);
        assertThat(((Number) row.get("double_major_id")).longValue()).isEqualTo(296002L);
        assertThat(((Number) row.get("advisor_id")).longValue()).isEqualTo(296001L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=296013 "
                + "AND action IN ('DOUBLE_MAJOR_REQUEST_APPROVED','STUDENT_DOUBLE_MAJOR_ASSIGNED')", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void existingDoubleMajorAndPrimaryMajorCollisionAreBlocked() {
        jdbc.update("UPDATE students SET double_major_id=296003 WHERE id=296001");
        assertThatThrownBy(() -> create("double-major-existing")).isInstanceOf(DoubleMajorConflictException.class);
        jdbc.update("UPDATE students SET double_major_id=NULL WHERE id=296001");
        assertThatThrownBy(() -> application.create(new DoubleMajorCreateRequestDTO(296001L),
                pdf("자기소개서.pdf"), pdf("학업계획서.pdf"), pdf("성적증명서.pdf"),
                "double-major-primary", STUDENT, CONTEXT)).isInstanceOf(DoubleMajorConflictException.class);
    }

    @Test
    void rejectionKeepsStudentAndOverlappingActivePeriodIsBlocked() {
        var created = create("double-major-create");
        var rejected = service.review(created.id(),
                new DoubleMajorReviewRequestDTO(AcademicChangeRequestStatus.REJECTED, "모집 기준 미충족"),
                "double-major-reject", ADMIN, CONTEXT);
        assertThat(rejected.status()).isEqualTo(AcademicChangeRequestStatus.REJECTED);
        assertThat(jdbc.queryForObject("SELECT double_major_id FROM students WHERE id=296001", Long.class)).isNull();

        LocalDateTime now = DoubleMajorPolicy.now().withNano(0);
        var overlapping = new DoubleMajorPeriodSaveRequestDTO(296002L, now.minusHours(1), now.plusHours(1),
                true, "겹치는 기간 등록");
        assertThatThrownBy(() -> periodService.create(overlapping, "double-major-period-overlap", ADMIN, CONTEXT))
                .isInstanceOf(DoubleMajorConflictException.class);
    }

    @Test
    void roleScopeAndOpenApiContractsAreExposed() throws Exception {
        create("double-major-create");
        PageResponseDTO<?> own = service.search(new DoubleMajorSearchRequestDTO(null, null, null, null,
                null, null, null, null, null), STUDENT, PageRequest.of(0, 20));
        assertThat(own.totalCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.search(new DoubleMajorSearchRequestDTO(null, null, null, null,
                null, null, null, null, null), PROFESSOR, PageRequest.of(0, 20)))
                .isInstanceOf(DoubleMajorAccessDeniedException.class);

        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['operationId']")
                        .value("createDoubleMajorRequest"))
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/review']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/documents/{documentType}']['get']['responses']['200']['content']['application/pdf']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/catalog/double-major-periods/{periodId}/status']['patch']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['targetMajorId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['targetSemesterId']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['reason']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorResponseDTO']['properties']['documents']").exists());
    }

    private DoubleMajorResponseDTO create(String key) {
        return application.create(body(), pdf("자기소개서.pdf"), pdf("학업계획서.pdf"), pdf("성적증명서.pdf"),
                key, STUDENT, CONTEXT);
    }

    private DoubleMajorCreateRequestDTO body() {
        return new DoubleMajorCreateRequestDTO(296002L);
    }

    private MockMultipartFile pdf(String filename) {
        return new MockMultipartFile("file", filename, "application/pdf",
                "%PDF-1.7 double-major".getBytes(StandardCharsets.UTF_8));
    }

    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM academic_change_request_files WHERE request_id IN "
                + "(SELECT id FROM academic_change_requests WHERE student_id IN (296001,296002))");
        jdbc.update("DELETE FROM academic_change_requests WHERE student_id IN (296001,296002)");
        jdbc.update("DELETE FROM academic_change_request_periods WHERE semester_id IN (296001,296002)");
        jdbc.update("DELETE FROM students WHERE id IN (296001,296002)");
        jdbc.update("DELETE FROM professors WHERE id=296001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM majors WHERE id BETWEEN 296001 AND 296003");
        jdbc.update("DELETE FROM departments WHERE id BETWEEN 296001 AND 296002");
        jdbc.update("DELETE FROM colleges WHERE id=296001");
        jdbc.update("DELETE FROM semesters WHERE id IN (296001,296002)");
    }
}
