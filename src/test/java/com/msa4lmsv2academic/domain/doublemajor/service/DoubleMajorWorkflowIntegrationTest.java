package com.msa4lmsv2academic.domain.doublemajor.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.request.AdminAcademicChangeRejectionRequestDTO;
import com.msa4lmsv2academic.domain.transfer.service.DepartmentTransferAuditContext;
import com.msa4lmsv2academic.global.error.*;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.io.IOException;
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
        jdbc.update("INSERT INTO users (id,name,role,status) VALUES "
                + "(296011,'복수전공학생','STUDENT','ACTIVE'),(296012,'다른학생','STUDENT','ACTIVE'),"
                + "(296013,'관리자','ADMIN','ACTIVE'),(296014,'지도교수','PROFESSOR','ACTIVE')");
        jdbc.update("INSERT INTO professors (id,version,user_id,hire_year,department_id) VALUES (296001,0,296014,2020,296001)");
        jdbc.update("INSERT INTO students (id,user_id,student_number,department_id,double_major_id,grade_level,admission_year,academic_status,advisor_id) VALUES "
                + "(296001,296011,'25961296001',296001,NULL,2,2025,'ENROLLED',296001),"
                + "(296002,296012,'25961296002',296001,NULL,2,2025,'ENROLLED',296001)");
        jdbc.update("INSERT INTO semesters (id,academic_year,term,start_date,end_date,enrollment_start_at,enrollment_end_at,is_current) "
                + "VALUES (295901,2025,'FIRST','2025-03-02','2025-06-18','2025-02-10 09:00:00','2025-02-14 18:00:00',0),"
                + "(295902,2025,'SECOND','2025-09-01','2025-12-17','2025-08-10 09:00:00','2025-08-14 18:00:00',0),"
                + "(296001,2027,'FIRST','2027-03-02','2027-06-18','2027-02-10 09:00:00','2027-02-14 18:00:00',0),"
                + "(296002,2027,'SECOND','2027-09-01','2027-12-17','2027-08-10 09:00:00','2027-08-14 18:00:00',0)");
        for (int index = 0; index < 11; index++) {
            long id = 296100L + index;
            long semesterId = index < 6 ? 295901L : 295902L;
            jdbc.update("INSERT INTO courses (id,department_id,code,name,credits,target_grade,completion_type) "
                            + "VALUES (?,?,?,?,3,1,'MAJOR_ELECTIVE')",
                    id, 296001L, "DM-" + index, "복수전공 자격과목 " + index);
            jdbc.update("INSERT INTO lectures (id,semester_id,course_id,professor_id,section_no,capacity,classroom,status,"
                            + "midterm_ratio,final_ratio,assignment_ratio,attendance_ratio) "
                            + "VALUES (?,?,?,?, '01',30,'101','CLOSED',30,30,30,10)",
                    id, semesterId, id, 296001L);
            jdbc.update("INSERT INTO enrollments (id,student_id,lecture_id,status,enrolled_at,grade_status,letter_grade) "
                            + "VALUES (?,?,?,'ACTIVE','2025-03-02 09:00:00','OPENED','A')",
                    id, 296001L, id);
        }
        LocalDateTime now = DoubleMajorPolicy.now().withNano(0);
        jdbc.update("INSERT INTO academic_change_request_periods "
                        + "(semester_id,request_type,start_at,end_at,is_active) VALUES (296001,'DOUBLE_MAJOR',?,?,1)",
                now.minusDays(1), now.plusDays(1));
        AtomicInteger sequence = new AtomicInteger();
        when(storage.upload(anyString(), any())).thenAnswer(invocation ->
                "double-major-requests/test/" + sequence.incrementAndGet() + ".hwp");
        when(storage.download(anyString())).thenReturn(new byte[] {1, 2, 3});
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void createUsesOpenRecruitmentPeriodWithoutTargetSemesterAndReplays() {
        var created = create("double-major-create");
        assertThat(created.status()).isEqualTo(AcademicChangeRequestStatus.PENDING);
        assertThat(created.studentNumber()).isEqualTo("25961296001");
        assertThat(created.targetDepartmentName()).isEqualTo("복수전공학과");
        assertThat(created.requestPeriodId()).isNotNull();
        assertThat(created.files()).hasSize(2);
        assertThat(create("double-major-create")).isEqualTo(created);
        verify(storage, times(2)).upload(anyString(), any());
        var row = jdbc.queryForMap("SELECT target_department_id,target_semester_id,request_period_id "
                        + "FROM academic_change_requests WHERE id=?",
                created.id());
        assertThat(((Number) row.get("target_department_id")).longValue()).isEqualTo(296002L);
        assertThat(row.get("target_semester_id")).isNull();
        assertThat(row.get("request_period_id")).isNotNull();
        assertThatThrownBy(() -> service.get(created.id(), OTHER)).isInstanceOf(DoubleMajorAccessDeniedException.class);
        assertThat(application.download(created.id(), created.files().get(1).id(), STUDENT).originalName())
                .isEqualTo("학업계획서 양식.hwp");
    }

    @Test
    void exactlyTwoHwpFilesAreRequiredBeforeRemoteUpload() {
        assertThatThrownBy(() -> application.create(body(), java.util.List.of(hwp("자기소개서 양식.hwp")),
                "double-major-missing", STUDENT, CONTEXT)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void twoCompletedRegularSemestersAreRequired() {
        jdbc.update("DELETE FROM enrollments WHERE id BETWEEN 296106 AND 296110");

        assertThatThrownBy(() -> create("double-major-semester-shortage"))
                .isInstanceOf(DoubleMajorConflictException.class)
                .hasMessageContaining("정규학기를 2개 이상");
        verifyNoInteractions(storage);
    }

    @Test
    void thirtyThreeEarnedCreditsAreRequired() {
        jdbc.update("UPDATE enrollments SET letter_grade='F' WHERE id=296110");

        assertThatThrownBy(() -> create("double-major-credit-shortage"))
                .isInstanceOf(DoubleMajorConflictException.class)
                .hasMessageContaining("33학점 이상");
        verifyNoInteractions(storage);
    }

    @Test
    void activeDepartmentTransferRequestBlocksDoubleMajorApplication() {
        jdbc.update("INSERT INTO academic_change_requests "
                        + "(student_id,request_type,source_department_id,target_department_id,target_semester_id,status) "
                        + "VALUES (296001,'TRANSFER_DEPARTMENT',296001,296002,296002,'PENDING')");

        assertThatThrownBy(() -> create("double-major-transfer-active"))
                .isInstanceOf(DoubleMajorConflictException.class)
                .hasMessageContaining("진행 중인 전과 또는 복수전공");
        verifyNoInteractions(storage);
    }

    @Test
    void advisorApprovalThenStampedFilesApplyDoubleMajorAndReplaceBothFiles() {
        var created = create("double-major-create");
        var advisorApproved = service.reviewByAdvisor(created.id(),
                new AdvisorDoubleMajorReviewRequestDTO(true, null),
                "double-major-advisor-approve", PROFESSOR, CONTEXT);
        assertThat(advisorApproved.status()).isEqualTo(AcademicChangeRequestStatus.ADVISOR_APPROVED);
        var applied = application.apply(created.id(), java.util.List.of(
                        hwp("자기소개서_학장날인.hwp"), hwp("학업계획서_학장날인.hwp")),
                "double-major-apply", ADMIN, CONTEXT);
        assertThat(applied.status()).isEqualTo(AcademicChangeRequestStatus.APPLIED);
        assertThat(applied.files()).extracting(file -> file.originalName())
                .containsExactlyInAnyOrder("자기소개서_학장날인.hwp", "학업계획서_학장날인.hwp");
        var row = jdbc.queryForMap(
                "SELECT student_number,department_id,double_major_id,advisor_id FROM students WHERE id=296001");
        assertThat(row.get("student_number")).isEqualTo("25961296001");
        assertThat(((Number) row.get("department_id")).longValue()).isEqualTo(296001L);
        assertThat(((Number) row.get("double_major_id")).longValue()).isEqualTo(296002L);
        assertThat(((Number) row.get("advisor_id")).longValue()).isEqualTo(296001L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE actor_id=296013 "
                + "AND action IN ('DOUBLE_MAJOR_REQUEST_APPLIED','STUDENT_DOUBLE_MAJOR_ASSIGNED')", Integer.class))
                .isEqualTo(2);
        verify(storage, times(2)).delete(startsWith("double-major-requests/test/"));
    }

    @Test
    void existingDoubleMajorAndPrimaryMajorCollisionAreBlocked() {
        jdbc.update("UPDATE students SET double_major_id=296002 WHERE id=296001");
        assertThatThrownBy(() -> create("double-major-existing")).isInstanceOf(DoubleMajorConflictException.class);
        jdbc.update("UPDATE students SET double_major_id=NULL WHERE id=296001");
        assertThatThrownBy(() -> application.create(new DoubleMajorCreateRequestDTO(296001L),
                java.util.List.of(hwp("자기소개서 양식.hwp"), hwp("학업계획서 양식.hwp")),
                "double-major-primary", STUDENT, CONTEXT)).isInstanceOf(DoubleMajorConflictException.class);
    }

    @Test
    void advisorAndDeanRejectionsRemainDistinctAndDoNotReplaceFiles() {
        var created = create("double-major-create");
        var advisorRejected = service.reviewByAdvisor(created.id(),
                new AdvisorDoubleMajorReviewRequestDTO(false, "학업계획 보완 필요"),
                "double-major-advisor-reject", PROFESSOR, CONTEXT);
        assertThat(advisorRejected.status()).isEqualTo(AcademicChangeRequestStatus.ADVISOR_REJECTED);

        var second = create("double-major-create-second");
        service.reviewByAdvisor(second.id(), new AdvisorDoubleMajorReviewRequestDTO(true, null),
                "double-major-advisor-approve-second", PROFESSOR, CONTEXT);
        var rejected = service.rejectByAdmin(second.id(),
                new AdminAcademicChangeRejectionRequestDTO("학장 날인 미확인"),
                "double-major-reject", ADMIN, CONTEXT);
        assertThat(rejected.status()).isEqualTo(AcademicChangeRequestStatus.REJECTED);
        assertThat(jdbc.queryForObject("SELECT double_major_id FROM students WHERE id=296002", Long.class)).isNull();
        verify(storage, never()).delete(anyString());

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
                null, null, null, null), STUDENT, PageRequest.of(0, 20));
        assertThat(own.totalCount()).isEqualTo(1);
        PageResponseDTO<?> advisorQueue = service.search(new DoubleMajorSearchRequestDTO(null, null, null, null,
                null, null, null, null), PROFESSOR, PageRequest.of(0, 20));
        assertThat(advisorQueue.totalCount()).isEqualTo(1);

        mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['operationId']")
                        .value("createDoubleMajorRequest"))
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/advisor-review']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/application']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/rejection']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/files/{fileId}']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/templates/study-plan']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests/{requestId}/cancellation']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/academic/catalog/double-major-periods/{periodId}/status']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['requestBody']"
                        + "['content']['multipart/form-data']['schema']['properties']['files']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/double-major-requests']['post']['requestBody']"
                        + "['content']['multipart/form-data']['schema']['properties']['transcript']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['targetDepartmentId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['targetMajorId']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['targetSemesterId']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorCreateRequestDTO']['properties']['reason']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorResponseDTO']['properties']['targetDepartmentId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorResponseDTO']['properties']['studentNumber']").exists())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorResponseDTO']['properties']['targetMajorId']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['DoubleMajorResponseDTO']['properties']['files']").exists());
    }

    private DoubleMajorResponseDTO create(String key) {
        return application.create(body(), java.util.List.of(
                        hwp("자기소개서 양식.hwp"), hwp("학업계획서 양식.hwp")),
                key, STUDENT, CONTEXT);
    }

    private DoubleMajorCreateRequestDTO body() {
        return new DoubleMajorCreateRequestDTO(296002L);
    }

    private MockMultipartFile hwp(String filename) {
        String resource = filename.startsWith("자기") ? "self-introduction.hwp" : "study-plan.hwp";
        try (var input = getClass().getClassLoader()
                .getResourceAsStream("templates/department-transfer/" + resource)) {
            if (input == null) throw new IllegalStateException("테스트 HWP 양식을 찾을 수 없습니다.");
            return new MockMultipartFile("files", filename, "application/x-hwp", input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void clean() {
        jdbc.update("DELETE FROM audit_logs WHERE actor_id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM idempotency_keys WHERE requester_user_id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM academic_change_request_files WHERE request_id IN "
                + "(SELECT id FROM academic_change_requests WHERE student_id IN (296001,296002))");
        jdbc.update("DELETE FROM academic_change_requests WHERE student_id IN (296001,296002)");
        jdbc.update("DELETE FROM academic_change_request_periods WHERE semester_id IN (296001,296002)");
        jdbc.update("DELETE FROM enrollments WHERE id BETWEEN 296100 AND 296110");
        jdbc.update("DELETE FROM lectures WHERE id BETWEEN 296100 AND 296110");
        jdbc.update("DELETE FROM courses WHERE id BETWEEN 296100 AND 296110");
        jdbc.update("DELETE FROM students WHERE id IN (296001,296002)");
        jdbc.update("DELETE FROM professors WHERE id=296001");
        jdbc.update("DELETE FROM users WHERE id BETWEEN 296011 AND 296014");
        jdbc.update("DELETE FROM departments WHERE id BETWEEN 296001 AND 296002");
        jdbc.update("DELETE FROM colleges WHERE id=296001");
        jdbc.update("DELETE FROM semesters WHERE id IN (295901,295902,296001,296002)");
    }
}
