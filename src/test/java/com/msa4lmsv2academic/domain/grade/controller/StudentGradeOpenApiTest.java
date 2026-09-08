package com.msa4lmsv2academic.domain.grade.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class StudentGradeOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsStudentGradeQueryContract() throws Exception {
        String path = "$['paths']['/api/academic/grades/me']['get']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['operationId']").value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['StudentGradeResponseDTO']"
                        + "['properties']['totalGpa']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentGradeResponseDTO']"
                        + "['properties']['queryGpa']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentGradeItemResponseDTO']"
                        + "['properties']['reflectedInGpa']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
