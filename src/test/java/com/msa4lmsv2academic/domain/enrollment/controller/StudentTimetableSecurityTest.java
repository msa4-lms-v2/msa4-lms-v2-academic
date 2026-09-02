package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StudentTimetableSecurityTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void professorCannotReadStudentTimetable() throws Exception {
        mockMvc.perform(get("/api/academic/timetables")
                        .param("academicYear", "2026")
                        .param("term", "FIRST")
                        .headers(gatewayHeaders(93102L, "PROFESSOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingGatewayHeadersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/academic/timetables")
                        .param("academicYear", "2026")
                        .param("term", "FIRST"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void missingSemesterConditionsAreRejected() throws Exception {
        mockMvc.perform(get("/api/academic/timetables")
                .headers(gatewayHeaders(93101L, "STUDENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
