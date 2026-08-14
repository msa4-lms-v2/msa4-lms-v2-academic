package com.msa4lmsv2academic.global.response;

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
class ErrorResponseCodeIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void businessExceptionUsesCommonExxMessage() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/notices/{noticeId}", Long.MAX_VALUE)
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"))
                .andExpect(jsonPath("$.message").value(CustomResponseCode.NOT_FOUND_DATA.getMessage()));
    }

    @Test
    void validationExceptionUsesCommonExxMessage() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/semesters")
                        .queryParam("page", "0")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"))
                .andExpect(jsonPath("$.message").value(CustomResponseCode.INVALID_PARAMETER.getMessage()));
    }

    @Test
    void securityExceptionUsesCommonExxMessage() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/semesters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"))
                .andExpect(jsonPath("$.message").value(CustomResponseCode.UNAUTHENTICATED.getMessage()));
    }
}
