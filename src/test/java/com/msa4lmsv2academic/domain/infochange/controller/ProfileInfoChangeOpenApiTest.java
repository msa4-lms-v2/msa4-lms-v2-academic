package com.msa4lmsv2academic.domain.infochange.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
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
class ProfileInfoChangeOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsProfileAndChangeRequestContracts() throws Exception {
        String studentRequests = "$['paths']['/api/academic/info-change-requests']";
        String professorRequests = "$['paths']['/api/academic/professor-info-change-requests']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/academic/students/me']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/professors/me']['get']").exists())
                .andExpect(jsonPath(studentRequests + "['get']").exists())
                .andExpect(jsonPath(studentRequests + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(studentRequests + "['post']['responses']['413']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/info-change-requests/{requestId}/cancel']['patch']")
                        .exists())
                .andExpect(jsonPath(professorRequests + "['get']").exists())
                .andExpect(jsonPath(professorRequests + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(professorRequests + "['post']['responses']['413']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/academic/professor-info-change-requests/{requestId}/cancel']['patch']"
                ).exists())
                .andExpect(jsonPath(studentRequests + "['get']['parameters'][*]['name']")
                        .value(hasItems("keyword", "status", "departmentId", "sortDirection", "page", "size")))
                .andExpect(jsonPath(professorRequests + "['get']['parameters'][*]['name']")
                        .value(hasItems("keyword", "status", "departmentId", "sortDirection", "page", "size")))
                .andExpect(jsonPath("$..operationId", hasItems(
                        "getMyStudentProfile",
                        "getMyProfessorProfile",
                        "createStudentProfileChangeRequest",
                        "createProfessorProfileChangeRequest"
                )))
                .andExpect(jsonPath("$..operationId", not(hasItems(containsStringIgnoringCase("scrum")))));
    }
}
