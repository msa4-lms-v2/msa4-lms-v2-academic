package com.msa4lmsv2academic.domain.student.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentDirectoryControllerTest extends MySqlIntegrationTest {

    private static final Long PROFESSOR_USER_ID = 9401L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Department department = Department.create("CSE-STUDENT-HTTP", null, "컴퓨터공학과", true);
        User professorUser = User.synchronize(
                PROFESSOR_USER_ID,
                "김교수",
                "professor.student-directory@test.com",
                null,
                null,
                UserRole.PROFESSOR,
                UserStatus.ACTIVE
        );
        entityManager.persist(department);
        entityManager.persist(professorUser);
        entityManager.persist(Professor.create(professorUser, (short) 2020, department));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void professorGetsEmptyPageWhenNoStudentsAreInScope() throws Exception {
        mockMvc.perform(get("/api/academic/students")
                        .headers(gatewayHeaders(PROFESSOR_USER_ID, "PROFESSOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void professorCannotRequestTerminalAcademicStatus() throws Exception {
        mockMvc.perform(get("/api/academic/students")
                        .queryParam("academicStatus", "WITHDRAWN")
                        .headers(gatewayHeaders(PROFESSOR_USER_ID, "PROFESSOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void adminCanRequestTerminalAcademicStatus() throws Exception {
        mockMvc.perform(get("/api/academic/students")
                        .queryParam("academicStatus", "WITHDRAWN")
                        .headers(gatewayHeaders(9402L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"));
    }

    @Test
    void invalidSortConditionReturns400() throws Exception {
        mockMvc.perform(get("/api/academic/students")
                        .queryParam("sortBy", "email")
                        .headers(gatewayHeaders(9402L, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void studentCannotUseStudentDirectory() throws Exception {
        mockMvc.perform(get("/api/academic/students")
                        .headers(gatewayHeaders(9403L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/academic/students"))
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
