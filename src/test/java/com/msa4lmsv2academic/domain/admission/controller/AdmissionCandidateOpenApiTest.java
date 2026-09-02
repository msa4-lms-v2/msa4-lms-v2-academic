package com.msa4lmsv2academic.domain.admission.controller;

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
class AdmissionCandidateOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsAdmissionCandidateContract() throws Exception {
        String collectionPath = "$['paths']['/api/academic/admission-candidates']";
        String itemPath = "$['paths']['/api/academic/admission-candidates/{candidateId}']";
        String statusPath = "$['paths']['/api/academic/admission-candidates/{candidateId}/status']";
        String summarySchema = "$['components']['schemas']['AdmissionCandidateSummaryResponseDTO']['properties']";
        String detailSchema = "$['components']['schemas']['AdmissionCandidateDetailResponseDTO']['properties']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value("searchAdmissionCandidates"))
                .andExpect(jsonPath(collectionPath + "['post']['operationId']")
                        .value("createAdmissionCandidate"))
                .andExpect(jsonPath(itemPath + "['get']['operationId']")
                        .value("getAdmissionCandidate"))
                .andExpect(jsonPath(itemPath + "['patch']['operationId']")
                        .value("updateAdmissionCandidate"))
                .andExpect(jsonPath(statusPath + "['patch']['operationId']")
                        .value("changeAdmissionCandidateStatus"))
                .andExpect(jsonPath(collectionPath + "['get']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(collectionPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(collectionPath + "['get']['parameters'][*]['name']").value(hasItems(
                        "page", "size", "keyword", "departmentId", "admissionYear", "status",
                        "sortBy", "sortDirection"
                )))
                .andExpect(jsonPath(collectionPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['400']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['401']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['403']").exists())
                .andExpect(jsonPath(collectionPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(summarySchema + "['birthDate']").doesNotExist())
                .andExpect(jsonPath(summarySchema + "['email']").doesNotExist())
                .andExpect(jsonPath(summarySchema + "['phoneNumber']").doesNotExist())
                .andExpect(jsonPath(summarySchema + "['address']").doesNotExist())
                .andExpect(jsonPath(detailSchema + "['birthDate']").exists())
                .andExpect(jsonPath(detailSchema + "['email']").exists())
                .andExpect(jsonPath(detailSchema + "['studentId']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdmissionCandidateCreateRequestDTO']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdmissionCandidateUpdateRequestDTO']").exists())
                .andExpect(jsonPath("$['components']['schemas']['AdmissionCandidateStatusRequestDTO']").exists())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
