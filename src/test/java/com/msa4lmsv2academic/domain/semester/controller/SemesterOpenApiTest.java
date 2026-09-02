package com.msa4lmsv2academic.domain.semester.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SemesterOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsSemesterContractsAndBearerSecurity() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/catalog/semesters']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/catalog/semesters']['post']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/credit-requirement-diagnosis']['get']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/common/credit-requirement-diagnosis']"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['get']['operationId']"
                ).value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['post']['operationId']"
                ).value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['get']['x-traceability']"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['post']['x-traceability']"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['get']['security'][0]['bearerAuth']"
                ).isArray())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/catalog/semesters']['post']['responses']['409']"
                ).exists())
                .andExpect(jsonPath(
                        "$['components']['schemas']['SemesterCreateRequestDTO']['properties']['academicYear']"
                ).exists())
                .andExpect(jsonPath(
                        "$['components']['schemas']['SemesterCreateRequestDTO']['required']"
                ).value(hasItems(
                        "academicYear",
                        "term",
                        "startDate",
                        "endDate",
                        "enrollmentStartAt",
                        "enrollmentEndAt"
                )))
                .andExpect(jsonPath(
                        "$['components']['securitySchemes']['bearerAuth']"
                ).exists());
    }
}
