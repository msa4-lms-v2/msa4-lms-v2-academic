package com.msa4lmsv2academic.domain.lecture.controller;

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
class LectureOpeningOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsLectureOpeningContracts() throws Exception {
        String requestsPath = "$['paths']['/api/academic/classes/opening-requests']";
        String approvalsPath = "$['paths']['/api/academic/classes/opening-approvals']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(requestsPath + "['get']").exists())
                .andExpect(jsonPath(requestsPath + "['post']").exists())
                .andExpect(jsonPath(approvalsPath + "['patch']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['400']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['401']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['403']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['404']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(approvalsPath + "['patch']['responses']['409']").exists())
                .andExpect(jsonPath(requestsPath + "['post']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['LectureOpeningCreateRequestDTO']"
                        + "['properties']['courseId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['LectureOpeningCreateRequestDTO']"
                        + "['properties']['schedules']").exists())
                .andExpect(jsonPath("$['components']['schemas']['LectureOpeningResponseDTO']"
                        + "['properties']['status']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
