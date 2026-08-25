package com.msa4lmsv2academic.domain.enrollment.controller;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnrollmentCreditLimitRuleSecurityTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanSearchRulesThroughGatewayHeaders() throws Exception {
        mockMvc.perform(get("/api/academic/enrollments/credit-limit-rules")
                        .headers(gatewayHeaders(3L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR"})
    void nonAdminCannotSearchRules(String role) throws Exception {
        mockMvc.perform(get("/api/academic/enrollments/credit-limit-rules")
                        .headers(gatewayHeaders(1L, role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingGatewayHeadersReturnAuthenticationRequired() throws Exception {
        mockMvc.perform(get("/api/academic/enrollments/credit-limit-rules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void incompleteGatewayHeadersReturnInvalidToken() throws Exception {
        mockMvc.perform(get("/api/academic/enrollments/credit-limit-rules")
                        .header("X-User-Id", "3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E04"));
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
