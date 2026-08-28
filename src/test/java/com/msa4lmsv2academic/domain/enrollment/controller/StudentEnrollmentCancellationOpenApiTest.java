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
class StudentEnrollmentCancellationOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsEnrollmentCancellationContract() throws Exception {
        String path = "$['paths']['/api/academic/enrollments/{enrollmentId}']['delete']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['responses']['409']").exists())
                .andExpect(jsonPath(path + "['operationId']").value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentCancellationResponseDTO']"
                        + "['properties']['enrollmentId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentCancellationResponseDTO']"
                        + "['properties']['status']").exists())
                .andExpect(jsonPath("$['components']['schemas']['StudentEnrollmentCancellationResponseDTO']"
                        + "['properties']['cancelledAt']").exists());
    }
}
