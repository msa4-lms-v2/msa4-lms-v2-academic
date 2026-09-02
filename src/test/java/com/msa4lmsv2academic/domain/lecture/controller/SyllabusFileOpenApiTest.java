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
class SyllabusFileOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsSyllabusFileContracts() throws Exception {
        String collectionPath = "$['paths']['/api/academic/classes/syllabus-files']";
        String downloadPath = "$['paths']['/api/academic/classes/syllabus-files/{fileId}']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']").exists())
                .andExpect(jsonPath(downloadPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['400']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['401']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['403']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['404']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['413']").exists())
                .andExpect(jsonPath(downloadPath + "['get']['responses']['200']['content']['application/pdf']")
                        .exists())
                .andExpect(jsonPath(collectionPath + "['post']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['SyllabusFileResponseDTO']"
                        + "['properties']['fileId']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
