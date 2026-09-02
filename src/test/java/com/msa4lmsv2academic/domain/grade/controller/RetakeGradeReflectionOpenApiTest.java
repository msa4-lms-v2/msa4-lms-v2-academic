package com.msa4lmsv2academic.domain.grade.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RetakeGradeReflectionOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsRetakeGradeReflectionContract() throws Exception {
        String path = "$['paths']['/api/academic/grades/{enrollmentId}/retake-reflection']['patch']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['responses']['409']").exists())
                .andExpect(jsonPath(path + "['operationId']").value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['RetakeGradeReflectionRequestDTO']"
                        + "['properties']['reason']").exists())
                .andExpect(jsonPath("$['components']['schemas']['RetakeGradeReflectionResponseDTO']"
                        + "['properties']['summaries']").exists());
    }

    @Test
    void professorCannotReflectRetakeGrade() throws Exception {
        mockMvc.perform(patch("/api/academic/grades/302/retake-reflection")
                        .headers(gatewayHeaders(2L, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"재수강 반영\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
