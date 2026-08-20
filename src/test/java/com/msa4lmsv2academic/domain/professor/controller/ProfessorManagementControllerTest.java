package com.msa4lmsv2academic.domain.professor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.professor.repository.ProfessorRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class ProfessorManagementControllerTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9401L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private Department currentDepartment;
    private Department targetDepartment;
    private Professor professor;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        professorRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();

        currentDepartment = departmentRepository.save(
                Department.create("209", null, "컴퓨터공학과", true)
        );
        targetDepartment = departmentRepository.save(
                Department.create("210", null, "인공지능학과", true)
        );

        entityManager.persist(User.synchronize(
                ADMIN_ID, "교수관리자", "professor-controller-admin@test.com", null, null,
                UserRole.ADMIN, UserStatus.ACTIVE
        ));
        User professorUser = User.synchronize(
                9402L, "김교수", "kim.controller@test.com", "010-1111-2222", "서울특별시 중구",
                UserRole.PROFESSOR, UserStatus.LOCKED
        );
        entityManager.persist(professorUser);
        professor = professorRepository.save(Professor.create(professorUser, (short) 2020, currentDepartment));
        entityManager.flush();
    }

    @Test
    void adminSearchesAndFiltersProfessorPage() throws Exception {
        mockMvc.perform(get("/api/academic/faculty-management")
                        .queryParam("keyword", "KIM.CONTROLLER")
                        .queryParam("status", "LOCKED")
                        .queryParam("departmentId", currentDepartment.getId().toString())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].professorId").value(professor.getId()))
                .andExpect(jsonPath("$.data.items[0].status").value("LOCKED"))
                .andExpect(jsonPath("$.data.items[0].phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].address").doesNotExist());
    }

    @Test
    void adminGetsProfessorDetailWithContactInformation() throws Exception {
        mockMvc.perform(get("/api/academic/faculty-management/{professorId}", professor.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.professorId").value(professor.getId()))
                .andExpect(jsonPath("$.data.userId").value(9402))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.address").value("서울특별시 중구"));
    }

    @Test
    void adminUpdatesEmploymentAndCreatesAuditLog() throws Exception {
        String body = """
                {
                  "departmentId": %d,
                  "hireYear": 2021,
                  "reason": "소속 학과 변경"
                }
                """.formatted(targetDepartment.getId());

        mockMvc.perform(patch("/api/academic/faculty-management/{professorId}", professor.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "professor-controller-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.departmentId").value(targetDepartment.getId()))
                .andExpect(jsonPath("$.data.departmentName").value("인공지능학과"))
                .andExpect(jsonPath("$.data.hireYear").value(2021));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    org.assertj.core.api.Assertions.assertThat(log.getActorId()).isEqualTo(ADMIN_ID);
                    org.assertj.core.api.Assertions.assertThat(log.getRequestId())
                            .isEqualTo("professor-controller-request");
                });
    }

    @Test
    void invalidPatchAndUnknownProfessorReturnDocumentedErrors() throws Exception {
        mockMvc.perform(patch("/api/academic/faculty-management/{professorId}", professor.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"변경 없음\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));

        mockMvc.perform(get("/api/academic/faculty-management/{professorId}", 999999)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR"})
    void nonAdminCannotUseFacultyManagement(String role) throws Exception {
        mockMvc.perform(get("/api/academic/faculty-management")
                        .headers(gatewayHeaders(100L, role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/academic/faculty-management"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
