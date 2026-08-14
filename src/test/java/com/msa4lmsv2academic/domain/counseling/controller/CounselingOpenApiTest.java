package com.msa4lmsv2academic.domain.counseling.controller;

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
class CounselingOpenApiTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatedOpenApiContainsAvailabilityAndAppointmentContracts() throws Exception {
        String availabilityPath = "$['paths']['/api/academic/counseling/availability']";
        String appointmentsPath = "$['paths']['/api/academic/counseling/appointments']";
        String appointmentPath = "$['paths']['/api/academic/counseling/appointments/{appointmentId}']";
        String statusPath = "$['paths']['/api/academic/counseling/appointments/{appointmentId}/status']";

        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(availabilityPath + "['get']").exists())
                .andExpect(jsonPath(availabilityPath + "['put']").exists())
                .andExpect(jsonPath(appointmentsPath + "['get']").exists())
                .andExpect(jsonPath(appointmentsPath + "['post']").exists())
                .andExpect(jsonPath(appointmentPath + "['get']").exists())
                .andExpect(jsonPath(statusPath + "['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/academic/counseling/records']").doesNotExist())
                .andExpect(jsonPath(appointmentsPath + "['post']['responses']['201']").exists())
                .andExpect(jsonPath(appointmentsPath + "['get']['security'][0]['bearerAuth']").isArray())
                .andExpect(jsonPath("$['components']['schemas']['CounselingAppointmentCreateRequestDTO']"
                        + "['required']").value(hasItems("professorId", "appointmentAt")))
                .andExpect(jsonPath("$['components']['schemas']['CounselingAppointmentStatusRequestDTO']"
                        + "['required']").value(hasItems("status")))
                .andExpect(jsonPath("$['components']['schemas']['CounselorAvailabilityReplaceRequestDTO']"
                        + "['required']").value(hasItems("slots")));
    }
}
