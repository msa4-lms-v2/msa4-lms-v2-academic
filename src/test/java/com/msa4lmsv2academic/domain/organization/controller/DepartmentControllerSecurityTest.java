package com.msa4lmsv2academic.domain.organization.controller;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.repository.CollegeRepository;
import com.msa4lmsv2academic.domain.organization.repository.DepartmentRepository;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerSecurityTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private College engineering;
    private Long inactiveDepartmentId;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAllInBatch();
        collegeRepository.deleteAllInBatch();

        engineering = collegeRepository.save(College.create("ENG", "공과대학", true));
        departmentRepository.save(Department.create("CSE", engineering, "컴퓨터공학과", true));
        inactiveDepartmentId = departmentRepository.saveAndFlush(
                Department.create("MEC", engineering, "기계공학과", false)
        ).getId();
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR", "ADMIN"})
    void getAllowsAllDocumentedRoles(String role) throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments")
                        .header("Authorization", bearer(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR"})
    void nonAdminCannotCreateOrUpdate(String role) throws Exception {
        String createBody = """
                {"code":"AIC","name":"인공지능학과","collegeId":%d}
                """.formatted(engineering.getId());

        mockMvc.perform(post("/api/academic/catalog/departments")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));

        mockMvc.perform(patch("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingAuthenticationReturns401AndE02() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void invalidTokenReturns401AndE04() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E04"));
    }

    @Test
    void tokenWithUnsupportedRoleReturns401AndE04() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments")
                        .header("Authorization", bearer("SYSTEM")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E04"));
    }

    @Test
    void studentCannotDiscoverInactiveDepartmentButAdminCan() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"));

        mockMvc.perform(get("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void adminCreateAllowsDocumentedCodeAndMissingCollege() throws Exception {
        String body = """
                {"code":"aic_01","name":"인공지능학과","active":false}
                """;

        mockMvc.perform(post("/api/academic/catalog/departments")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.code").value("aic_01"))
                .andExpect(jsonPath("$.data.college").doesNotExist())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void adminPatchReturns200AndRejectsEmptyOrCodeField() throws Exception {
        mockMvc.perform(patch("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"기계시스템공학과\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.code").value("MEC"))
                .andExpect(jsonPath("$.data.name").value("기계시스템공학과"));

        mockMvc.perform(patch("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));

        mockMvc.perform(patch("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));

        mockMvc.perform(patch("/api/academic/catalog/departments/{id}", inactiveDepartmentId)
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collegeId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void adminCreateRejectsCodeLongerThanTwentyCharacters() throws Exception {
        mockMvc.perform(post("/api/academic/catalog/departments")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456789012345678901\",\"name\":\"컴퓨터공학과\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void listClampsSizeTo100AndKeepsStudentScopeActiveOnly() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/departments")
                        .queryParam("size", "500")
                        .queryParam("active", "false")
                        .header("Authorization", bearer("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].code").value("CSE"));
    }

    private String bearer(String role) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("1")
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(jwtSigningKey())
                .compact();
        return "Bearer " + token;
    }
}
