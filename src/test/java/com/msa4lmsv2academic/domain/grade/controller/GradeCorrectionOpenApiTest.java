package com.msa4lmsv2academic.domain.grade.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GradeCorrectionOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsGradeCorrectionContracts() throws Exception {
        String correctionPath = "$['paths']['/api/academic/grades/corrections']";
        String requestSchema = "$['components']['schemas']['GradeCorrectionRequestDTO']";
        String itemSchema = "$['components']['schemas']['GradeCorrectionItemRequestDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(correctionPath + "['get']").exists())
                .andExpect(jsonPath(correctionPath + "['patch']").exists())
                .andExpect(jsonPath(correctionPath
                        + "['patch']['parameters'][?(@.name == 'Idempotency-Key')].required")
                        .value(hasItems(true)))
                .andExpect(jsonPath(correctionPath + "['patch']['responses']['200']").exists())
                .andExpect(jsonPath(correctionPath + "['patch']['responses']['409']").exists())
                .andExpect(jsonPath(requestSchema + "['required']")
                        .value(hasItems("classId", "corrections")))
                .andExpect(jsonPath(itemSchema + "['required']")
                        .value(hasItems("enrollmentId", "midtermScore", "finalScore",
                                "assignmentScore", "attendanceScore", "reason")));
    }
}
