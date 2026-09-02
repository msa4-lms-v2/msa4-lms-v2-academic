package com.msa4lmsv2academic.domain.academicschedule.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
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
class AcademicScheduleOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsAcademicScheduleContractsAndSchemas() throws Exception {
        String collectionPath = "$['paths']['/api/academic/academic-schedules']";
        String itemPath = "$['paths']['/api/academic/academic-schedules/{scheduleId}']";
        String statusPath = "$['paths']['/api/academic/academic-schedules/{scheduleId}/status']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']").exists())
                .andExpect(jsonPath(itemPath + "['get']").exists())
                .andExpect(jsonPath(itemPath + "['put']").exists())
                .andExpect(jsonPath(itemPath + "['delete']").doesNotExist())
                .andExpect(jsonPath(statusPath + "['patch']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(itemPath + "['get']['responses']['403']").exists())
                .andExpect(jsonPath(itemPath + "['get']['responses']['404']").exists())
                .andExpect(jsonPath(itemPath + "['put']['responses']['409']").exists())
                .andExpect(jsonPath(statusPath + "['patch']['responses']['409']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(collectionPath + "['post']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['put']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(statusPath + "['patch']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['AcademicScheduleCreateRequestDTO']['required']")
                        .value(hasItems("title", "startDate", "targetRole")))
                .andExpect(jsonPath("$['components']['schemas']['AcademicScheduleUpdateRequestDTO']['required']")
                        .value(hasItems("title", "startDate", "targetRole", "reason")))
                .andExpect(jsonPath("$['components']['schemas']['AcademicScheduleStatusRequestDTO']['required']")
                        .value(hasItems("active", "reason")))
                .andExpect(jsonPath("$['components']['schemas']['AcademicScheduleSummaryResponseDTO']"
                        + "['properties']['content']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['AcademicScheduleDetailResponseDTO']"
                        + "['properties']['content']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
