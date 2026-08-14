package com.msa4lmsv2academic.domain.academicschedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import com.msa4lmsv2academic.domain.academicschedule.repository.AcademicScheduleRepository;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademicScheduleControllerTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9601L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private User admin;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        academicScheduleRepository.deleteAllInBatch();
        admin = User.synchronize(
                ADMIN_ID,
                "학사일정 관리자",
                "academic-schedule-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        entityManager.persist(admin);
        entityManager.flush();
    }

    @Test
    void studentListAppliesRoleActiveOverlapKeywordAndSortingRules() throws Exception {
        save("수강신청 안내", "전체 대상", LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22),
                AcademicScheduleTargetRole.ALL, true);
        save("수강신청 사전 안내", "학생 대상", LocalDate.of(2026, 8, 10), null,
                AcademicScheduleTargetRole.STUDENT, true);
        save("수강신청 교수 안내", "교수 대상", LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 25),
                AcademicScheduleTargetRole.PROFESSOR, true);
        save("수강신청 비공개", "학생 대상", LocalDate.of(2026, 8, 12), null,
                AcademicScheduleTargetRole.STUDENT, false);
        save("성적 입력", "다른 키워드", LocalDate.of(2026, 8, 18), null,
                AcademicScheduleTargetRole.ALL, true);

        mockMvc.perform(get("/api/academic/academic-schedules")
                        .queryParam("keyword", "수강신청")
                        .queryParam("from", "2026-08-09")
                        .queryParam("to", "2026-08-21")
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[*].title").value(contains(
                        "수강신청 사전 안내", "수강신청 안내"
                )))
                .andExpect(jsonPath("$.data.items[*].targetRole").value(containsInAnyOrder(
                        "ALL", "STUDENT"
                )))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].authorId").doesNotExist());
    }

    @Test
    void adminCreatesUpdatesChangesStatusAndRecordsOnlyActualChanges() throws Exception {
        String createdResponse = mockMvc.perform(post("/api/academic/academic-schedules")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "academic-schedule-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  2026학년도 2학기 수강신청  ",
                                  "content": "  신청 기간을 확인하세요.  ",
                                  "startDate": "2026-08-17",
                                  "endDate": "2026-08-21",
                                  "targetRole": "STUDENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.title").value("2026학년도 2학기 수강신청"))
                .andExpect(jsonPath("$.data.content").value("신청 기간을 확인하세요."))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn().getResponse().getContentAsString();

        long scheduleId = ((Number) JsonPath.read(createdResponse, "$.data.id")).longValue();

        String updateBody = """
                {
                  "title": "2026학년도 2학기 수강신청 변경",
                  "content": "신청 기간이 연장되었습니다.",
                  "startDate": "2026-08-17",
                  "endDate": "2026-08-22",
                  "targetRole": "ALL",
                  "reason": "신청 기간 연장"
                }
                """;
        mockMvc.perform(put("/api/academic/academic-schedules/{scheduleId}", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endDate").value("2026-08-22"))
                .andExpect(jsonPath("$.data.targetRole").value("ALL"));

        mockMvc.perform(put("/api/academic/academic-schedules/{scheduleId}", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        String inactiveBody = """
                {"active": false, "reason": "일정 취소"}
                """;
        mockMvc.perform(patch("/api/academic/academic-schedules/{scheduleId}/status", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        mockMvc.perform(patch("/api/academic/academic-schedules/{scheduleId}/status", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveBody))
                .andExpect(status().isOk());

        String activeBody = """
                {"active": true, "reason": "일정 재개"}
                """;
        mockMvc.perform(patch("/api/academic/academic-schedules/{scheduleId}/status", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));

        mockMvc.perform(patch("/api/academic/academic-schedules/{scheduleId}/status", scheduleId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody))
                .andExpect(status().isOk());

        assertThat(auditLogRepository.findAll())
                .extracting(log -> log.getAction())
                .containsExactly(
                        "ACADEMIC_SCHEDULE_CREATE",
                        "ACADEMIC_SCHEDULE_UPDATE",
                        "ACADEMIC_SCHEDULE_STATUS_CHANGE",
                        "ACADEMIC_SCHEDULE_STATUS_CHANGE"
                );
    }

    @Test
    void inactiveScheduleIsHiddenFromStudentButVisibleToAdmin() throws Exception {
        AcademicSchedule inactive = save(
                "비활성 일정", null, LocalDate.of(2026, 9, 1), null,
                AcademicScheduleTargetRole.STUDENT, false
        );

        mockMvc.perform(get("/api/academic/academic-schedules/{scheduleId}", inactive.getId())
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"));

        mockMvc.perform(get("/api/academic/academic-schedules/{scheduleId}", inactive.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.authorId").value(ADMIN_ID));
    }

    @Test
    void duplicateInvalidAndUnauthorizedRequestsAreRejected() throws Exception {
        String createBody = validCreateBody("중복 일정");
        mockMvc.perform(post("/api/academic/academic-schedules")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/academic/academic-schedules")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));

        mockMvc.perform(post("/api/academic/academic-schedules")
                        .headers(gatewayHeaders(100L, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("교수 등록 시도")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));

        mockMvc.perform(post("/api/academic/academic-schedules")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "잘못된 기간",
                                  "startDate": "2026-08-22",
                                  "endDate": "2026-08-21",
                                  "targetRole": "ALL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));

        mockMvc.perform(get("/api/academic/academic-schedules")
                        .queryParam("targetRole", "STUDENT")
                        .headers(gatewayHeaders(100L, "PROFESSOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/academic/academic-schedules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    private AcademicSchedule save(String title, String content, LocalDate startDate, LocalDate endDate,
                                  AcademicScheduleTargetRole role, boolean active) {
        AcademicSchedule schedule = AcademicSchedule.create(title, content, startDate, endDate, role, admin);
        if (!active) {
            schedule.changeActive(false);
        }
        return academicScheduleRepository.saveAndFlush(schedule);
    }

    private String validCreateBody(String title) {
        return """
                {
                  "title": "%s",
                  "content": "동일 본문",
                  "startDate": "2026-08-17",
                  "endDate": "2026-08-21",
                  "targetRole": "ALL"
                }
                """.formatted(title);
    }

    private HttpHeaders gatewayHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        headers.set("X-User-Role", role);
        return headers;
    }
}
