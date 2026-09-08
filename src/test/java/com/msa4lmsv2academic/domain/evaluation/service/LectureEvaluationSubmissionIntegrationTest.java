package com.msa4lmsv2academic.domain.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LectureEvaluationSubmissionIntegrationTest extends MySqlIntegrationTest {

    private static final long PROFESSOR_USER_ID = 96101L;
    private static final long STUDENT_USER_ID = 96102L;
    private static final long OTHER_STUDENT_USER_ID = 96103L;
    private static final long SEMESTER_ID = 96101L;
    private static final long ENROLLMENT_ID = 96101L;
    private static final long OTHER_ENROLLMENT_ID = 96102L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now(KST);
        jdbcTemplate.update("INSERT INTO colleges (id, code, name, active) "
                + "VALUES (96101, 'EVAL-COL', '강의평가대학', 1)");
        jdbcTemplate.update("INSERT INTO departments (id, code, college_id, name, active) "
                + "VALUES (96101, 'E01', 96101, '강의평가학과', 1)");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (96101, '평가담당교수', 'PROFESSOR', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (96102, '평가학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO users (id, name, role, status) "
                + "VALUES (96103, '다른학생', 'STUDENT', 'ACTIVE')");
        jdbcTemplate.update("INSERT INTO professors (id, version, user_id, hire_year, department_id) "
                + "VALUES (96101, 0, 96101, 2020, 96101)");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status) "
                + "VALUES (96101, 96102, 96101, 2, 2025, 'ENROLLED')");
        jdbcTemplate.update("INSERT INTO students "
                + "(id, user_id, department_id, grade_level, admission_year, academic_status) "
                + "VALUES (96102, 96103, 96101, 2, 2025, 'ENROLLED')");
        jdbcTemplate.update("INSERT INTO semesters "
                        + "(id, academic_year, term, start_date, end_date, enrollment_start_at, "
                        + "enrollment_end_at, evaluation_start_at, evaluation_end_at, is_current) "
                        + "VALUES (?, 2026, 'FIRST', ?, ?, ?, ?, ?, ?, 0)",
                SEMESTER_ID,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 12, 18),
                now.minusMonths(7),
                now.minusMonths(6),
                now.minusDays(1),
                now.plusDays(1));
        jdbcTemplate.update("INSERT INTO courses "
                + "(id, department_id, code, name, credits, target_grade, completion_type) "
                + "VALUES (96101, 96101, 'EVAL-101', '강의평가대상과목', 3, 2, 'MAJOR_REQUIRED')");
        jdbcTemplate.update("INSERT INTO lectures "
                + "(id, semester_id, course_id, professor_id, section_no, capacity, classroom, status, "
                + "midterm_ratio, final_ratio, assignment_ratio, attendance_ratio, syllabus) "
                + "VALUES (96101, 96101, 96101, 96101, '01', 40, '공학관 301호', 'OPEN', "
                + "30, 30, 30, 10, '강의계획서')");
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, 96101, 96101, 'ACTIVE', ?, 'DRAFT')",
                ENROLLMENT_ID, now.minusMonths(6));
        jdbcTemplate.update("INSERT INTO enrollments "
                        + "(id, student_id, lecture_id, status, enrolled_at, grade_status) "
                        + "VALUES (?, 96102, 96101, 'ACTIVE', ?, 'DRAFT')",
                OTHER_ENROLLMENT_ID, now.minusMonths(6));
    }

    @Test
    void persistsRatingsAsJsonAndBlocksSecondSubmission() throws Exception {
        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(STUDENT_USER_ID, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(ENROLLMENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.enrollmentId").value(ENROLLMENT_ID));

        String ratings = jdbcTemplate.queryForObject(
                "SELECT CAST(ratings AS CHAR) FROM lecture_evaluations WHERE enrollment_id = ?",
                String.class,
                ENROLLMENT_ID
        );
        assertThat(ratings).contains("CONTENT_QUALITY", "DELIVERY_CLARITY");

        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(STUDENT_USER_ID, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(ENROLLMENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    @Test
    void hidesAnotherStudentsEnrollment() throws Exception {
        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(STUDENT_USER_ID, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(OTHER_ENROLLMENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"));
    }

    @Test
    void rejectsSubmissionOutsideConfiguredEvaluationPeriod() throws Exception {
        LocalDateTime now = LocalDateTime.now(KST);
        jdbcTemplate.update("UPDATE semesters SET evaluation_start_at = ?, evaluation_end_at = ? WHERE id = ?",
                now.minusDays(2), now.minusDays(1), SEMESTER_ID);

        mockMvc.perform(post("/api/academic/evaluations")
                        .headers(gatewayHeaders(STUDENT_USER_ID, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(ENROLLMENT_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    private String validBody(long enrollmentId) {
        return """
                {
                  "enrollmentId": %d,
                  "ratings": {
                    "CONTENT_QUALITY": 5,
                    "DELIVERY_CLARITY": 4
                  },
                  "comment": "실습 예제가 좋았습니다."
                }
                """.formatted(enrollmentId);
    }

    private HttpHeaders gatewayHeaders(long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
