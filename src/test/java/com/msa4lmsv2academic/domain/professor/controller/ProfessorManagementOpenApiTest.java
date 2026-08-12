package com.msa4lmsv2academic.domain.professor.controller;

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
class ProfessorManagementOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsFacultyManagementContractsAndNoExternalPost() throws Exception {
        String collectionPath = "$['paths']['/api/academic/faculty-management']";
        String itemPath = "$['paths']['/api/academic/faculty-management/{professorId}']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']").doesNotExist())
                .andExpect(jsonPath(itemPath + "['get']").exists())
                .andExpect(jsonPath(itemPath + "['patch']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(itemPath + "['patch']['responses']['400']").exists())
                .andExpect(jsonPath(itemPath + "['patch']['responses']['404']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(itemPath + "['patch']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['ProfessorUpdateRequestDTO']['required']")
                        .value(hasItems("reason")))
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
