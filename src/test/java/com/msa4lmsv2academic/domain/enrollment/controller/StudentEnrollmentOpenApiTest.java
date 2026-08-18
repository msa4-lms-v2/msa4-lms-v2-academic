package com.msa4lmsv2academic.domain.enrollment.controller;

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
class StudentEnrollmentOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsStudentEnrollmentContract() throws Exception {
        String enrollmentsPath = "$['paths']['/api/academic/enrollments']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(enrollmentsPath + "['get']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(enrollmentsPath + "['get']['responses']['200']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['responses']['400']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['responses']['401']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['responses']['403']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['responses']['404']").exists())
                .andExpect(jsonPath(enrollmentsPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentResponseDTO']"
                        + "['properties']['enrollmentId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentResponseDTO']"
                        + "['properties']['enrollmentStatus']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentResponseDTO']"
                        + "['properties']['classId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentResponseDTO']"
                        + "['properties']['courseName']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
