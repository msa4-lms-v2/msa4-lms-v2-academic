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
class ClassOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsStudentClassContract() throws Exception {
        String classesPath = "$['paths']['/api/academic/classes']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(classesPath + "['get']").exists())
                .andExpect(jsonPath(classesPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(classesPath + "['get']['responses']['200']").exists())
                .andExpect(jsonPath(classesPath + "['get']['responses']['400']").exists())
                .andExpect(jsonPath(classesPath + "['get']['responses']['401']").exists())
                .andExpect(jsonPath(classesPath + "['get']['responses']['403']").exists())
                .andExpect(jsonPath(classesPath + "['get']['responses']['404']").exists())
                .andExpect(jsonPath(classesPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['StudentClassResponseDTO']"
                        + "['properties']['classId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentClassResponseDTO']"
                        + "['properties']['courseName']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
