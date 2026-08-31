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
        String attachment = "$['paths']['/api/academic/withdrawals/{withdrawalId}/attachment']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collection + "['get']").exists())
                .andExpect(jsonPath(collection + "['post']").exists())
                .andExpect(jsonPath(detail + "['get']").exists())
                .andExpect(jsonPath(detail + "['get']['responses']['403']['description']")
                        .value("E03: 역할·본인·지도교수 범위 위반"))
                .andExpect(jsonPath(detail + "['get']['responses']['404']['description']")
                        .value("E10: 자퇴 신청 또는 증빙 없음"))
                .andExpect(jsonPath(detail + "['get']['responses']['403']['content']['*/*']['schema']").exists())
                .andExpect(jsonPath(detail + "['get']['responses']['404']['content']['*/*']['schema']").exists())
                .andExpect(jsonPath(advisorReview + "['patch']").exists())
                .andExpect(jsonPath(finalReview + "['patch']").exists())
                .andExpect(jsonPath(attachment + "['put']['operationId']").value("updateWithdrawalAttachment"))
                .andExpect(jsonPath(attachment + "['get']['operationId']").value("downloadWithdrawalAttachment"))
                .andExpect(jsonPath(attachment + "['put']['responses']['200']").exists())
                .andExpect(jsonPath(attachment + "['put']['responses']['413']").exists())
                .andExpect(jsonPath(attachment + "['get']['responses']['200']['content']['application/pdf']").exists())
                .andExpect(jsonPath(attachment + "['put']['parameters'][?(@.name == 'Idempotency-Key')].required")
                        .value(hasItems(true)))
                .andExpect(jsonPath(collection + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collection + "['post']['responses']['200']").doesNotExist())
                .andExpect(jsonPath(collection + "['get']['responses']['200']").exists())
                .andExpect(jsonPath(advisorReview + "['patch']['responses']['200']").exists())
                .andExpect(jsonPath(finalReview + "['patch']['responses']['200']").exists())
                .andExpect(jsonPath(collection + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(collection + "['post']['operationId']").value("createWithdrawal"))
                .andExpect(jsonPath(collection + "['post']['parameters'][?(@.name == 'Idempotency-Key')].required")
                        .value(hasItems(true)))
                .andExpect(jsonPath("$['paths']['/api/academic/withdrawals/{withdrawalId}/status']['patch']['operationId']")
                        .value("cancelWithdrawal"))
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalCancelRequestDTO']['required']")
                        .value(hasItems("cancelReason")))
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalResponseDTO']['properties']['cancelledBy']").exists())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalResponseDTO']['properties']['attachmentOriginalName']").exists())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalResponseDTO']['properties']['attachmentStoredName']")
                        .doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalAttachmentUpdateRequestDTO']['properties']['changeReason']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawalCreateRequestDTO']['required']")
                        .value(hasItems("reason")))
                .andExpect(jsonPath("$['components']['schemas']['AdvisorWithdrawalReviewRequestDTO']['required']")
                        .value(hasItems("approved")))
                .andExpect(jsonPath("$['components']['schemas']['FinalWithdrawalReviewRequestDTO']['required']")
                        .value(hasItems("approved")));
    }
}
