package com.msa4lmsv2academic.domain.attendance.controller;

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
class ExcuseRequestOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsExcuseStatusQueryContract() throws Exception {
        String path = "$['paths']['/api/academic/attendance/excuses']['get']";
        String responseSchema = "$['components']['schemas']['ExcuseRequestStatusResponseDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['parameters'][?(@.name == 'status')]").exists())
                .andExpect(jsonPath(responseSchema + "['properties']['studentName']").exists())
                .andExpect(jsonPath(responseSchema + "['properties']['courseName']").exists())
                .andExpect(jsonPath(responseSchema + "['properties']['rejectReason']").exists())
                .andExpect(jsonPath(responseSchema + "['properties']['attachmentOriginalName']").exists());
    }

    @Test
    void generatedOpenApiContainsExcuseApplicationContract() throws Exception {
        String path = "$['paths']['/api/academic/attendance/excuses']['post']";
        String requestSchema = "$['components']['schemas']['ExcuseRequestCreateRequestDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['201']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['responses']['409']").exists())
                .andExpect(jsonPath(requestSchema + "['required']")
                        .value(hasItems("enrollmentId", "lectureDate", "period", "reason")))
                .andExpect(jsonPath(requestSchema + "['properties']['enrollmentId']['example']").value(12001))
                .andExpect(jsonPath(requestSchema + "['properties']['lectureDate']['example']")
                        .value("2026-09-01"))
                .andExpect(jsonPath(requestSchema + "['properties']['reason']['maxLength']").value(500));
    }

    @Test
    void generatedOpenApiContainsExcuseAttachmentContract() throws Exception {
        String attachmentPath = "$['paths']['/api/academic/attendance/excuses/{requestId}/attachment']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(attachmentPath + "['put']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(attachmentPath + "['put']['requestBody']['content']['multipart/form-data']")
                        .exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['200']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['400']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['401']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['403']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['404']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['409']").exists())
                .andExpect(jsonPath(attachmentPath + "['put']['responses']['413']").exists())
                .andExpect(jsonPath(attachmentPath + "['get']").exists())
                .andExpect(jsonPath(attachmentPath + "['get']['responses']['200']['content']['application/pdf']")
                        .exists());
    }

    @Test
    void generatedOpenApiContainsProfessorExcuseReviewContract() throws Exception {
        String path = "$['paths']['/api/academic/attendance/excuses/{requestId}']['patch']";
        String requestSchema = "$['components']['schemas']['ExcuseReviewRequestDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['parameters'][?(@.name == 'Idempotency-Key')].required")
                        .value(hasItems(true)))
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['responses']['409']").exists())
                .andExpect(jsonPath(requestSchema + "['required']").value(hasItems("status")))
                .andExpect(jsonPath(requestSchema + "['properties']['rejectReason']['maxLength']").value(500));
    }
}
