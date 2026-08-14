package com.msa4lmsv2academic.domain.withdrawal.controller;

import static org.hamcrest.Matchers.hasItems;
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
class WithdrawalOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsTwoStageWithdrawalWorkflow() throws Exception {
        String collection = "$['paths']['/api/academic/withdrawals']";
        String detail = "$['paths']['/api/academic/withdrawals/{withdrawalId}']";
        String advisorReview = "$['paths']['/api/academic/withdrawals/{withdrawalId}/advisor-review']";
        String finalReview = "$['paths']['/api/academic/withdrawals/{withdrawalId}/final-review']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collection + "['get']").exists())
                .andExpect(jsonPath(collection + "['post']").exists())
                .andExpect(jsonPath(detail + "['get']").exists())
                .andExpect(jsonPath(advisorReview + "['patch']").exists())
                .andExpect(jsonPath(finalReview + "['patch']").exists())
                .andExpect(jsonPath(collection + "['post']['responses']['201']").exists())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalCreateRequestDTO']['required']")
                        .value(hasItems("reason")))
                .andExpect(jsonPath("$['components']['schemas']['AdvisorWithdrawalReviewRequestDTO']['required']")
                        .value(hasItems("approved")))
                .andExpect(jsonPath("$['components']['schemas']['FinalWithdrawalReviewRequestDTO']['required']")
                        .value(hasItems("approved")));
    }
}
