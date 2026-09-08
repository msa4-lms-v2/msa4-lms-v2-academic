package com.msa4lmsv2academic.domain.evaluation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.evaluation.response.LectureEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.evaluation.response.ProfessorLectureEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.evaluation.service.LectureEvaluationService;
import com.msa4lmsv2academic.domain.evaluation.service.ProfessorLectureEvaluationQueryService;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    @MockitoBean
    private ProfessorLectureEvaluationQueryService professorLectureEvaluationQueryService;

    @Test
    void professorQueriesOwnedLectureEvaluationResultsWith200() throws Exception {
        CurrentUser professor = new CurrentUser(20L, "PROFESSOR");
        var item = new ProfessorLectureEvaluationResponseDTO(
                21L,
                "CSE301",
                "소프트웨어공학",
                "01",
                (short) 2026,
                SemesterTerm.FIRST,
                40L,
                32L,
                new BigDecimal("80.00"),
                true,
                new BigDecimal("4.25"),
                Map.of("CONTENT_QUALITY", new BigDecimal("4.30")),
                List.of("실습 예제가 좋았습니다.")
        );
        when(professorLectureEvaluationQueryService.getMyResults(any(), eq(professor)))
                .thenReturn(new PageResponseDTO<>(List.of(item), 1L, 1, 20, false));

        mockMvc.perform(get("/api/academic/evaluations")
                        .headers(gatewayHeaders(20L, "PROFESSOR"))
                        .param("lectureId", "21")
                        .param("academicYear", "2026")
                        .param("term", "FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.items[0].lectureId").value(21))
                .andExpect(jsonPath("$.data.items[0].responseCount").value(32))
                .andExpect(jsonPath("$.data.items[0].overallAverage").value(4.25))
                .andExpect(jsonPath("$.data.items[0].comments[0]").value("실습 예제가 좋았습니다."))
                .andExpect(jsonPath("$.data.items[0].studentId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].enrollmentId").doesNotExist());
    }

    @Test
    void studentCannotQueryProfessorEvaluationResults() throws Exception {
        mockMvc.perform(get("/api/academic/evaluations")
                        .headers(gatewayHeaders(10L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void invalidProfessorEvaluationSearchConditionReturns400() throws Exception {
        mockMvc.perform(get("/api/academic/evaluations")
                        .headers(gatewayHeaders(20L, "PROFESSOR"))
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

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
