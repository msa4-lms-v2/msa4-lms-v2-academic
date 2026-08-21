package com.msa4lmsv2academic.domain.graduation.controller;

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
class GraduationOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsCreditDiagnosisAndRequirementContracts() throws Exception {
        String diagnosisPath = "$['paths']['/api/academic/credit-requirement-diagnoses']['get']";
        String singleDiagnosisPath = "$['paths']['/api/academic/credit-requirement-diagnosis']['get']";
        String requirementsPath = "$['paths']['/api/academic/catalog/graduation-requirements']";
        String requirementPath =
                "$['paths']['/api/academic/catalog/graduation-requirements/{requirementId}']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(diagnosisPath + "['operationId']")
                        .value("searchCreditRequirementDiagnoses"))
                .andExpect(jsonPath(singleDiagnosisPath + "['operationId']")
                        .value("getCreditRequirementDiagnosis"))
                .andExpect(jsonPath(requirementsPath + "['get']['operationId']")
                        .value("searchGraduationRequirements"))
                .andExpect(jsonPath(requirementsPath + "['post']['operationId']")
                        .value("createGraduationRequirement"))
                .andExpect(jsonPath(requirementPath + "['get']['operationId']")
                        .value("getGraduationRequirement"))
                .andExpect(jsonPath(requirementPath + "['patch']['operationId']")
                        .value("updateGraduationRequirement"))
                .andExpect(jsonPath(diagnosisPath + "['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(requirementsPath + "['post']['operationId']")
                        .value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(diagnosisPath + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(requirementsPath + "['post']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(diagnosisPath + "['parameters'][*]['name']").value(hasItems(
                        "page", "size", "keyword", "departmentId", "admissionYear",
                        "academicStatus", "diagnosisStatus", "sortBy", "sortDirection"
                )))
                .andExpect(jsonPath(requirementsPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(requirementsPath + "['post']['responses']['409']").exists())
                .andExpect(jsonPath(requirementPath + "['patch']['responses']['404']").exists())
                .andExpect(jsonPath("$['components']['schemas']['GraduationRequirementCreateRequestDTO']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['GraduationRequirementUpdateRequestDTO']")
                        .exists())
                .andExpect(jsonPath("$['components']['schemas']['CreditDiagnosisSummaryResponseDTO']")
                        .exists());
    }
}
