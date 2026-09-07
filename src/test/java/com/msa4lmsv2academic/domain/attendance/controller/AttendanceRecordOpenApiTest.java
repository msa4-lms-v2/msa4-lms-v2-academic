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
class AttendanceRecordOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsAttendanceRecordQueryContract() throws Exception {
        String path = "$['paths']['/api/academic/attendance/records']['get']";
        String schema = "$['components']['schemas']['AttendanceRecordResponseDTO']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).exists())
                .andExpect(jsonPath(path + "['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath(path + "['responses']['200']").exists())
                .andExpect(jsonPath(path + "['responses']['400']").exists())
                .andExpect(jsonPath(path + "['responses']['401']").exists())
                .andExpect(jsonPath(path + "['responses']['403']").exists())
                .andExpect(jsonPath(path + "['parameters'][?(@.name == 'classId')]").exists())
                .andExpect(jsonPath(path + "['parameters'][?(@.name == 'status')]").exists())
                .andExpect(jsonPath(schema + "['properties']['studentName']").exists())
                .andExpect(jsonPath(schema + "['properties']['courseName']").exists())
                .andExpect(jsonPath(schema + "['properties']['modified']").exists());
    }

    @Test
    void generatedOpenApiContainsAttendanceRecordUpdateContract() throws Exception {
        String path = "$['paths']['/api/academic/attendance/records/{attendanceId}']['patch']";
        String schema = "$['components']['schemas']['AttendanceRecordUpdateRequestDTO']";

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
                .andExpect(jsonPath(schema + "['required']").value(hasItems("status", "reason")))
                .andExpect(jsonPath(schema + "['properties']['remarks']['maxLength']").value(255))
                .andExpect(jsonPath(schema + "['properties']['reason']['maxLength']").value(255));
    }
}
