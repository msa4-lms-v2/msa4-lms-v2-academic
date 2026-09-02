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
class EnrollmentCartOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsEnrollmentCartContract() throws Exception {
        String collectionPath = "$['paths']['/api/academic/enrollment-cart-items']";
        String itemPath = "$['paths']['/api/academic/enrollment-cart-items/{cartItemId}']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']").exists())
                .andExpect(jsonPath(collectionPath + "['post']").exists())
                .andExpect(jsonPath(itemPath + "['delete']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['400']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['401']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['403']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['404']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(itemPath + "['delete']['responses']['409']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCartSummaryResponseDTO']"
                        + "['properties']['totalCredits']").exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCartItemResponseDTO']"
                        + "['properties']['schedules']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
