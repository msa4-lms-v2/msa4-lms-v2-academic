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
class GradeManagementOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsGradeManagementContracts() throws Exception {
        String gradePath = "$['paths']['/api/academic/grades']";
        String statusPath = "$['paths']['/api/academic/grades/classes/{classId}/status']['patch']";
        String saveSchema = "$['components']['schemas']['GradeSaveRequestDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(gradePath + "['get']").exists())
                .andExpect(jsonPath(gradePath + "['post']").exists())
                .andExpect(jsonPath(gradePath + "['patch']").exists())
                .andExpect(jsonPath(statusPath).exists())
                .andExpect(jsonPath(gradePath + "['post']['parameters'][?(@.name == 'Idempotency-Key')].required")
                        .value(hasItems(true)))
                .andExpect(jsonPath(gradePath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(gradePath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(saveSchema + "['required']").value(hasItems("classId", "grades")));
    }
}
