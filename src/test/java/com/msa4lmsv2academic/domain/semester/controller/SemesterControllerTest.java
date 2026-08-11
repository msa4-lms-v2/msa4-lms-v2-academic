package com.msa4lmsv2academic.domain.semester.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SemesterControllerTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9101L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        semesterRepository.deleteAllInBatch();
        entityManager.persist(User.synchronize(
                ADMIN_ID,
                "학사관리자",
                "semester-controller-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        ));
        entityManager.flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR", "ADMIN"})
    void getAllowsAllDocumentedRolesAndReturnsEmptyPage(String role) throws Exception {
        mockMvc.perform(get("/api/academic/catalog/semesters")
                        .queryParam("academicYear", "2026")
                        .queryParam("term", "FIRST")
                        .queryParam("isCurrent", "true")
                        .header("Authorization", bearer(100L, role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "PROFESSOR"})
    void nonAdminCannotCreateSemester(String role) throws Exception {
        mockMvc.perform(post("/api/academic/catalog/semesters")
                        .header("Authorization", bearer(100L, role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(2026, "FIRST", true)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void adminCreatesSemesterWith201AndAuditLog() throws Exception {
        mockMvc.perform(post("/api/academic/catalog/semesters")
                        .header("Authorization", bearer(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "semester-controller-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(2026, "FIRST", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.academicYear").value(2026))
                .andExpect(jsonPath("$.data.term").value("FIRST"))
                .andExpect(jsonPath("$.data.isCurrent").value(true));

        assertThatAuditWasRecorded();
    }

    @Test
    void duplicateSemesterReturns409() throws Exception {
        mockMvc.perform(post("/api/academic/catalog/semesters")
                        .header("Authorization", bearer(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(2026, "FIRST", false)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/academic/catalog/semesters")
                        .header("Authorization", bearer(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(2026, "FIRST", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    @Test
    void invalidPeriodOrderReturns400() throws Exception {
        String body = """
                {
                  "academicYear": 2026,
                  "term": "FIRST",
                  "startDate": "2026-06-19",
                  "endDate": "2026-03-02",
                  "enrollmentStartAt": "2026-02-20T18:00:00",
                  "enrollmentEndAt": "2026-02-16T09:00:00",
                  "isCurrent": false
                }
                """;

        mockMvc.perform(post("/api/academic/catalog/semesters")
                        .header("Authorization", bearer(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/semesters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    private void assertThatAuditWasRecorded() {
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.findAll())
                .singleElement()
                .satisfies(log -> {
                    org.assertj.core.api.Assertions.assertThat(log.getAction()).isEqualTo("SEMESTER_CREATE");
                    org.assertj.core.api.Assertions.assertThat(log.getRequestId())
                            .isEqualTo("semester-controller-request");
                });
    }

    private String validBody(int academicYear, String term, boolean current) {
        return """
                {
                  "academicYear": %d,
                  "term": "%s",
                  "startDate": "2026-03-02",
                  "endDate": "2026-06-19",
                  "enrollmentStartAt": "2026-02-16T09:00:00",
                  "enrollmentEndAt": "2026-02-20T18:00:00",
                  "isCurrent": %s
                }
                """.formatted(academicYear, term, current);
    }

    private String bearer(Long userId, String role) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(jwtSigningKey())
                .compact();
        return "Bearer " + token;
    }
}
