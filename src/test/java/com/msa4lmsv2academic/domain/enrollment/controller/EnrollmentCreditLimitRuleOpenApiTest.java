package com.msa4lmsv2academic.domain.enrollment.controller;

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
class EnrollmentCreditLimitRuleOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsEnrollmentCreditLimitRuleContracts() throws Exception {
        String collectionPath =
                "$['paths']['/api/academic/enrollments/credit-limit-rules']";
        String detailPath =
                "$['paths']['/api/academic/enrollments/credit-limit-rules/{ruleId}']";
        String statusPath =
                "$['paths']['/api/academic/enrollments/credit-limit-rules/{ruleId}/status']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value("searchEnrollmentCreditLimitRules"))
                .andExpect(jsonPath(collectionPath + "['post']['operationId']")
                        .value("createEnrollmentCreditLimitRule"))
                .andExpect(jsonPath(detailPath + "['get']['operationId']")
                        .value("getEnrollmentCreditLimitRule"))
                .andExpect(jsonPath(detailPath + "['put']['operationId']")
                        .value("updateEnrollmentCreditLimitRule"))
                .andExpect(jsonPath(statusPath + "['patch']['operationId']")
                        .value("changeEnrollmentCreditLimitRuleStatus"))
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['get']['parameters'][*]['name']").value(hasItems(
                        "page", "size", "academicYear", "term", "active", "sortBy", "sortDirection"
                )))
                .andExpect(jsonPath(collectionPath + "['get']['responses']['200']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['responses']['400']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['responses']['401']").exists())
                .andExpect(jsonPath(collectionPath + "['get']['responses']['403']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCreditLimitRuleCreateRequestDTO']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCreditLimitRuleUpdateRequestDTO']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCreditLimitRuleStatusRequestDTO']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['EnrollmentCreditLimitRuleResponseDTO']")
                        .exists());
    }
}
