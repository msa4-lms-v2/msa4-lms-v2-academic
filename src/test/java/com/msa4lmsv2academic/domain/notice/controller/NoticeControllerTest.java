package com.msa4lmsv2academic.domain.notice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import com.msa4lmsv2academic.domain.notice.repository.NoticeRepository;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
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
class NoticeControllerTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9401L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    private User admin;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        noticeRepository.deleteAllInBatch();
        admin = User.synchronize(
                ADMIN_ID,
                "공지관리자",
                "notice-controller-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        entityManager.persist(admin);
        entityManager.flush();
    }

    @Test
    void studentListReturnsOnlyAllowedActiveSummaries() throws Exception {
        save("전체 공지", "목록에 노출되면 안 되는 본문", NoticeTargetRole.ALL, true);
        save("학생 공지", "학생 본문", NoticeTargetRole.STUDENT, true);
        save("교수 공지", "교수 본문", NoticeTargetRole.PROFESSOR, true);
        save("비활성 학생 공지", "비활성", NoticeTargetRole.STUDENT, false);

        mockMvc.perform(get("/api/academic/catalog/notices")
                        .queryParam("keyword", "공지")
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].authorId").doesNotExist())
                .andExpect(jsonPath("$.data.items[*].targetRole").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "ALL", "STUDENT"
                )));
    }

    @Test
    void studentCannotFilterOrReadProfessorNotice() throws Exception {
        Notice professorNotice = save("교수 공지", "내용", NoticeTargetRole.PROFESSOR, true);

        mockMvc.perform(get("/api/academic/catalog/notices")
                        .queryParam("targetRole", "PROFESSOR")
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));

        mockMvc.perform(get("/api/academic/catalog/notices/{noticeId}", professorNotice.getId())
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));
    }

    @Test
    void inactiveDetailIsHiddenFromStudentButVisibleToAdmin() throws Exception {
        Notice inactive = save("비활성 공지", "숨김", NoticeTargetRole.STUDENT, false);

        mockMvc.perform(get("/api/academic/catalog/notices/{noticeId}", inactive.getId())
                        .headers(gatewayHeaders(100L, "STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E10"));

        mockMvc.perform(get("/api/academic/catalog/notices/{noticeId}", inactive.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("숨김"))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.authorId").doesNotExist());
    }

    @Test
    void adminCreatesUpdatesAndSoftDeletesNotice() throws Exception {
        String createBody = """
                {
                  "title": "  등록 공지  ",
                  "content": "등록 본문",
                  "targetRole": "ALL"
                }
                """;

        String response = mockMvc.perform(post("/api/academic/catalog/notices")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .header("X-Request-Id", "notice-controller-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.title").value("등록 공지"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number createdId = com.jayway.jsonpath.JsonPath.read(response, "$.data.id");
        long noticeId = createdId.longValue();

        mockMvc.perform(patch("/api/academic/catalog/notices/{noticeId}", noticeId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 공지",
                                  "targetRole": "STUDENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정 공지"))
                .andExpect(jsonPath("$.data.targetRole").value("STUDENT"));

        mockMvc.perform(delete("/api/academic/catalog/notices/{noticeId}", noticeId)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        assertThatNoticeIsInactive(noticeId);
        org.assertj.core.api.Assertions.assertThat(auditLogRepository.findAll())
                .extracting(log -> log.getAction())
                .containsExactly("NOTICE_CREATE", "NOTICE_UPDATE", "NOTICE_DELETE");
    }

    @Test
    void mutationRejectsNonAdminAndRepeatedDeleteReturns409() throws Exception {
        mockMvc.perform(post("/api/academic/catalog/notices")
                        .headers(gatewayHeaders(100L, "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("교수 작성")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("E03"));

        Notice inactive = save("삭제된 공지", "내용", NoticeTargetRole.ALL, false);
        mockMvc.perform(delete("/api/academic/catalog/notices/{noticeId}", inactive.getId())
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("E11"));
    }

    @Test
    void repeatedAndInvalidRequestsFollowNoticeRules() throws Exception {
        mockMvc.perform(post("/api/academic/catalog/notices")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("중복 공지")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/academic/catalog/notices")
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("중복 공지")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("00"));

        mockMvc.perform(patch("/api/academic/catalog/notices/{noticeId}", 1L)
                        .headers(gatewayHeaders(ADMIN_ID, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E21"));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/academic/catalog/notices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E02"));
    }

    private Notice save(String title, String content, NoticeTargetRole role, boolean active) {
        Notice notice = Notice.create(title, content, role, admin);
        if (!active) {
            notice.deactivate();
        }
        return noticeRepository.saveAndFlush(notice);
    }

    private void assertThatNoticeIsInactive(long noticeId) {
        entityManager.clear();
        org.assertj.core.api.Assertions.assertThat(noticeRepository.findById(noticeId))
                .get()
                .extracting(Notice::isActive)
                .isEqualTo(false);
    }

    private String validCreateBody(String title) {
        return """
                {
                  "title": "%s",
                  "content": "동일 본문",
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
