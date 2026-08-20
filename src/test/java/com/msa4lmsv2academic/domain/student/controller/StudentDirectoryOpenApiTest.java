package com.msa4lmsv2academic.domain.student.controller;

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
class StudentDirectoryOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsStudentDirectoryContract() throws Exception {
        String path = "$['paths']['/api/academic/students']['get']";
        String schema = "$['components']['schemas']['StudentSummaryResponseDTO']['properties']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['operationId']").value("searchStudents"))
                .andExpect(jsonPath(path + "['operationId']").value(not(containsString("SCRUM"))))
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['responses']['404']").exists())
                .andExpect(jsonPath(path + "['parameters'][*]['name']").value(hasItems(
                        "page", "size", "keyword", "departmentId", "gradeLevel", "admissionYear",
                        "academicStatus", "sortBy", "sortDirection"
                )))
                .andExpect(jsonPath(schema + "['studentId']").exists())
                .andExpect(jsonPath(schema + "['userId']").exists())
                .andExpect(jsonPath(schema + "['departmentName']").exists())
                .andExpect(jsonPath(schema + "['doubleMajorName']").exists())
                .andExpect(jsonPath(schema + "['academicStatus']").exists())
                .andExpect(jsonPath(schema + "['advisorName']").exists())
                .andExpect(jsonPath(schema + "['email']").doesNotExist())
                .andExpect(jsonPath(schema + "['phoneNumber']").doesNotExist())
                .andExpect(jsonPath(schema + "['address']").doesNotExist())
                .andExpect(jsonPath("$['components']['securitySchemes']['bearerAuth']").exists());
    }
}
