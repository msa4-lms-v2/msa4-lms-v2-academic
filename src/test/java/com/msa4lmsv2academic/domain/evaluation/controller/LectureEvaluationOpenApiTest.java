package com.msa4lmsv2academic.domain.evaluation.controller;

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
class LectureEvaluationOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsLectureEvaluationSubmissionContract() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/evaluations']['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/evaluations']['post']['operationId']"
                ).value("submitLectureEvaluation"))
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/evaluations']['post']['security'][0]['bearerAuth']"
                ).isArray())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/evaluations']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/evaluations']['post']['responses']['409']"
                ).exists())
                .andExpect(jsonPath(
                        "$['components']['schemas']['LectureEvaluationSubmitRequestDTO']['required']"
                ).value(hasItems("enrollmentId", "ratings")));
    }
}
