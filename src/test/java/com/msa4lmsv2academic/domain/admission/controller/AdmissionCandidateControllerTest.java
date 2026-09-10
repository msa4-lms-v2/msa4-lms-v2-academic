package com.msa4lmsv2academic.domain.admission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.msa4lmsv2academic.domain.admission.repository.AdmissionCandidateRepository;
import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdmissionCandidateControllerTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 99001L;
    private static final int ADMISSION_YEAR = Year.now().getValue() + 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdmissionCandidateRepository admissionCandidateRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private com.msa4lmsv2academic.domain.outbox.repository.OutboxEventRepository outboxEventRepository;

    @Autowired
    private com.msa4lmsv2academic.domain.provisioning.service.AccountProvisioningService provisioningService;

    private Department department;
    private Professor advisor;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        admissionCandidateRepository.deleteAllInBatch();

        User administrator = User.synchronize(
                ADMIN_ID,
                "입학관리자",
                "admission-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        entityManager.persist(administrator);
        department = departmentRepository.saveAndFlush(
                Department.create("091", null, "입학테스트학과", true)
        );
        User advisorUser = User.synchronize(
                99002L, "지도교수", "advisor@test.com", null, null,
                UserRole.PROFESSOR, UserStatus.ACTIVE
        );
        entityManager.persist(advisorUser);
        advisor = Professor.create(advisorUser, (short) 2020, department);
        entityManager.persist(advisor);
        entityManager.flush();
    }

    @Test
    void adminCreatesCandidateAndQueuesAccountWithoutApplicationNumber() throws Exception {
        long candidateId = createCandidate(" APP-TEST-001 ", " 김민수 ", "minsu@test.com");

        mockMvc.perform(get("/api/academic/admission-candidates/{candidateId}", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationNumber").doesNotExist())
                .andExpect(jsonPath("$.data.name").value("김민수"))
                .andExpect(jsonPath("$.data.email").value("minsu@test.com"))
                .andExpect(jsonPath("$.data.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.data.admissionYear").value(ADMISSION_YEAR))
                .andExpect(jsonPath("$.data.status").value("PROVISIONING"))
                .andExpect(jsonPath("$.data.studentId").value(nullValue()));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getAction()).isEqualTo("ADMISSION_CANDIDATE_CREATE");
        assertThat(logs.getFirst().getReason()).isNull();
        assertThat(logs.getFirst().getAfterValue().toString())
                .doesNotContain("APP-TEST-001", "김민수", "2008-03-15", "minsu@test.com");
    }

    @Test
    void pendingRegistrationCanBeRetriedThenCancelledThroughDetailActions() throws Exception {
        long id = createCandidate("retry", "재시도", "retry@test.com");
        mockMvc.perform(post("/api/academic/admission-candidates/{id}/provisioning/retry", id)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PROVISIONING"));
        mockMvc.perform(post("/api/academic/admission-candidates/{id}/provisioning/cancel", id)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        entityManager.flush();
        entityManager.clear();
        var events = outboxEventRepository.lockAdmissionRequests(id);
        assertThat(events).hasSize(2).allSatisfy(e -> assertThat(e.getLastErrorCode()).isEqualTo("CANCELLED_BY_ADMIN"));
        assertThat(outboxEventRepository.findAll()).anySatisfy(e -> {
            assertThat(e.getAggregateId()).isEqualTo(id);
            assertThat(e.getEventType()).isEqualTo("AdmissionCandidateCancelled");
        });
        mockMvc.perform(post("/api/academic/admission-candidates/{id}/provisioning/retry", id)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void studentCannotRetryOrCancelAdmission() throws Exception {
        for (var action : List.of("retry", "cancel")) {
            mockMvc.perform(post("/api/academic/admission-candidates/1/provisioning/" + action)
                            .headers(gatewayHeaders(99003L, "STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void registrationQueuesAccountAndProvisioningLinksGeneratedStudent() throws Exception {
        long candidateId = createCandidate("queue", "자동등록", "automatic@test.com");
        var event = outboxEventRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("AdmissionCandidateRegistered") && e.getAggregateId().equals(candidateId))
                .findFirst().orElseThrow();
        assertThat(event.getPayload()).containsEntry("admissionCandidateId", candidateId)
                .containsEntry("administratorId", ADMIN_ID);
        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수정시도\"}"))
                .andExpect(status().isConflict());
        var response = provisioningService.provisionStudent(new com.msa4lmsv2academic.domain.provisioning.request.StudentProvisioningRequestDTO(
                9001L, "자동등록", "automatic@test.com", null, null, department.getId(), (short) ADMISSION_YEAR, candidateId));
        entityManager.flush();
        entityManager.clear();
        mockMvc.perform(get("/api/academic/admission-candidates/{candidateId}", candidateId).headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROVISIONED"))
                .andExpect(jsonPath("$.data.studentNumber").value(response.loginId()))
                .andExpect(jsonPath("$.data.accountId").value(9001));
        assertThat(response.loginId()).matches("[0-9]{8}");
    }

    @Test
    void listSupportsKeywordAndDoesNotExposePersonalInformation() throws Exception {
        createCandidate("APP-SEARCH-002", "이영희", "younghee@test.com");
        createCandidate("APP-SEARCH-001", "김민수", "minsu-search@test.com");

        mockMvc.perform(get("/api/academic/admission-candidates")
                        .queryParam("keyword", "")
                        .queryParam("departmentId", department.getId().toString())
                        .queryParam("admissionYear", String.valueOf(ADMISSION_YEAR))
                        .queryParam("status", "PROVISIONING")
                        .queryParam("sortBy", "name")
                        .queryParam("sortDirection", "asc")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items[*].name", contains(
                        "김민수", "이영희"
                )))
                .andExpect(jsonPath("$.data.items[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].address").doesNotExist());
    }

    @Test
    void registeredCandidateCanBePartiallyUpdatedWithoutReasonAndBlankOptionalValueClearsIt() throws Exception {
        long candidateId = createCandidate("APP-UPDATE-001", "수정전", "before@test.com");
        // 기존 REGISTERED 데이터의 수정 동작은 유지한다.
        org.springframework.test.util.ReflectionTestUtils.setField(admissionCandidateRepository.findById(candidateId).orElseThrow(), "status", com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus.REGISTERED);
        int auditCountBeforeUpdate = auditLogRepository.findAll().size();

        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "admission-update-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "수정후",
                                  "email": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정후"))
                .andExpect(jsonPath("$.data.email").value(nullValue()));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(auditCountBeforeUpdate + 1);
        AuditLog updateLog = logs.getLast();
        assertThat(updateLog.getAction()).isEqualTo("ADMISSION_CANDIDATE_UPDATE");
        assertThat(updateLog.getReason()).isNull();
        assertThat(updateLog.getAfterValue().get("changedFields").toString())
                .contains("name", "email");
        assertThat(updateLog.getAfterValue().toString()).doesNotContain("수정후", "before@test.com");
    }

    @Test
    void statusChangeRequiresReasonAndConfirmedCandidateCannotBeEdited() throws Exception {
        long candidateId = createCandidate("APP-STATUS-001", "상태대상", null);

        org.springframework.test.util.ReflectionTestUtils.setField(admissionCandidateRepository.findById(candidateId).orElseThrow(), "status", com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus.REGISTERED);
        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}/status", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED",
                                  "reason": "합격 자료 검증 완료"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.statusChangedBy").value(ADMIN_ID));

        int auditCountAfterConfirmation = auditLogRepository.findAll().size();
        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}/status", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED",
                                  "reason": "동일 요청"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        assertThat(auditLogRepository.findAll()).hasSize(auditCountAfterConfirmation);

        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"변경불가\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    @Test
    void duplicateEmailAndSystemOnlyStatusAreRejected() throws Exception {
        long candidateId = createCandidate("APP-DUPLICATE-001", "중복원본", null);

        mockMvc.perform(post("/api/academic/admission-candidates")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("APP-DUPLICATE-001", "중복요청", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));

        mockMvc.perform(patch("/api/academic/admission-candidates/{candidateId}/status", candidateId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PROVISIONED",
                                  "reason": "관리자가 직접 프로비저닝 시도"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    @Test
    void endpointRequiresAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/academic/admission-candidates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));

        mockMvc.perform(get("/api/academic/admission-candidates")
                        .headers(gatewayHeaders(500L, "PROFESSOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    private long createCandidate(String applicationNumber, String name, String email) throws Exception {
        String response = mockMvc.perform(post("/api/academic/admission-candidates")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "admission-create-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(applicationNumber, name, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }

    private String createBody(String applicationNumber, String name, String email) {
        String emailProperty = email == null ? ",\n  \"email\": \"" + applicationNumber.toLowerCase() + "@test.com\"" : ",\n  \"email\": \"" + email + "\"";
        return """
                {
                  "name": "%s",
                  "birthDate": "2008-03-15",
                  "departmentId": %d,
                  "advisorProfessorId": %d,
                  "admissionYear": %d%s
                }
                """.formatted(name, department.getId(), advisor.getId(), ADMISSION_YEAR, emailProperty);
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
