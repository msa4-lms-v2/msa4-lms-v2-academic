package com.msa4lmsv2academic.domain.evaluation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.evaluation.response.LectureEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.evaluation.service.LectureEvaluationService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LectureEvaluationControllerTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LectureEvaluationService lectureEvaluationService;

    @Test
    void studentSubmitsValidEvaluationWith201() throws Exception {
        CurrentUser student = new CurrentUser(10L, "STUDENT");
        when(lectureEvaluationService.submit(any(), eq(student))).thenReturn(
                new LectureEvaluationResponseDTO(
                        41L,
                        301L,
                        21L,
                        LocalDateTime.of(2026, 6, 10, 14, 30)
                )
        );

        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(10L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.evaluationId").value(41))
                .andExpect(jsonPath("$.data.enrollmentId").value(301))
                .andExpect(jsonPath("$.data.lectureId").value(21));
    }

    @Test
    void professorCannotSubmitEvaluation() throws Exception {
        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(20L, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/api/academic/evaluations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    @Test
    void invalidRatingsReturn400BeforeServiceCall() throws Exception {
        String body = """
                {
                  "enrollmentId": 301,
                  "ratings": {"CONTENT_QUALITY": 0},
                  "comment": "의견"
                }
                """;

        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(10L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    private String validBody() {
        return """
                {
                  "enrollmentId": 301,
                  "ratings": {
                    "CONTENT_QUALITY": 5,
                    "DELIVERY_CLARITY": 4
                  },
                  "comment": "실습 예제가 좋았습니다."
                }
                """;
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
