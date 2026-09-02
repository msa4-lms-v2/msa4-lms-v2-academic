package com.msa4lmsv2academic.domain.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import com.msa4lmsv2academic.domain.notice.repository.NoticeRepository;
import com.msa4lmsv2academic.domain.notice.request.NoticeCreateRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeSearchRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeUpdateRequestDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeDetailResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.msa4lmsv2academic.domain.user.entity.UserStatus;
import com.msa4lmsv2academic.global.error.InvalidNoticeRequestException;
import com.msa4lmsv2academic.global.error.NoticeAccessDeniedException;
import com.msa4lmsv2academic.global.error.NoticeNotFoundException;
import com.msa4lmsv2academic.global.error.NoticeStateConflictException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import com.msa4lmsv2academic.support.MySqlIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class NoticeServiceTest extends MySqlIntegrationTest {

    private static final Long ADMIN_ID = 9301L;
    private static final CurrentUser ADMIN = new CurrentUser(ADMIN_ID, "ADMIN");
    private static final CurrentUser STUDENT = new CurrentUser(9302L, "STUDENT");
    private static final CurrentUser PROFESSOR = new CurrentUser(9303L, "PROFESSOR");

    @Autowired
    private NoticeService noticeService;

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
                "notice-service-admin@test.com",
                null,
                null,
                UserRole.ADMIN,
                UserStatus.ACTIVE
        );
        entityManager.persist(admin);
        entityManager.flush();
    }

    @Test
    void createNormalizesInputsAndRecordsAuditMetadata() {
        NoticeDetailResponseDTO created = noticeService.createNotice(
                new NoticeCreateRequestDTO("  전체 공지  ", "   ", NoticeTargetRole.ALL),
                ADMIN,
                "notice-create-request",
                "127.0.0.1"
        );

        assertThat(created.title()).isEqualTo("전체 공지");
        assertThat(created.content()).isNull();
        assertThat(created.isActive()).isTrue();
        assertThat(created.createdAt()).isNotNull();

        Notice saved = noticeRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getAuthor().getId()).isEqualTo(ADMIN_ID);

        AuditLog auditLog = auditLogRepository.findAll().getFirst();
        assertThat(auditLog.getAction()).isEqualTo("NOTICE_CREATE");
        assertThat(auditLog.getTargetType()).isEqualTo("NOTICE");
        assertThat(auditLog.getTargetId()).isEqualTo(created.id());
        assertThat(auditLog.getAfterValue()).containsEntry("authorId", ADMIN_ID);
        assertThat(auditLog.getRequestId()).isEqualTo("notice-create-request");
        assertThat(auditLog.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void createAllowsRepeatedTitleContentAndTargetRole() {
        NoticeDetailResponseDTO original = create("재공지", "같은 내용", NoticeTargetRole.STUDENT);
        NoticeDetailResponseDTO repeated = create("재공지", "같은 내용", NoticeTargetRole.STUDENT);

        assertThat(repeated.id()).isNotEqualTo(original.id());
        assertThat(repeated.isActive()).isTrue();
    }

    @Test
    void mutationRejectsContentLongerThanFiveThousandCharacters() {
        String oversizedContent = "가".repeat(5001);

        assertThatThrownBy(() -> noticeService.createNotice(
                new NoticeCreateRequestDTO("긴 공지", oversizedContent, NoticeTargetRole.ALL),
                ADMIN,
                null,
                null
        )).isInstanceOf(InvalidNoticeRequestException.class);

        NoticeDetailResponseDTO notice = create("수정 대상", "본문", NoticeTargetRole.ALL);
        assertThatThrownBy(() -> noticeService.updateNotice(
                notice.id(),
                new NoticeUpdateRequestDTO(null, oversizedContent, null, null),
                ADMIN,
                null,
                null
        )).isInstanceOf(InvalidNoticeRequestException.class);
    }

    @Test
    void searchRestrictsGeneralUsersAndLetsAdminSeeAllStatuses() {
        create("전체 공지", "전체 내용", NoticeTargetRole.ALL);
        NoticeDetailResponseDTO studentNotice = create("학생 공지", "학생 내용", NoticeTargetRole.STUDENT);
        create("교수 공지", "교수 내용", NoticeTargetRole.PROFESSOR);
        noticeService.deleteNotice(studentNotice.id(), ADMIN, null, null);

        PageResponseDTO<NoticeSummaryResponseDTO> studentResult = noticeService.searchNotices(
                search(null, null),
                STUDENT
        );
        assertThat(studentResult.items())
                .extracting(NoticeSummaryResponseDTO::title)
                .containsExactly("전체 공지");

        PageResponseDTO<NoticeSummaryResponseDTO> professorResult = noticeService.searchNotices(
                search(NoticeTargetRole.PROFESSOR, false),
                PROFESSOR
        );
        assertThat(professorResult.items())
                .extracting(NoticeSummaryResponseDTO::title)
                .containsExactly("교수 공지");

        PageResponseDTO<NoticeSummaryResponseDTO> adminResult = noticeService.searchNotices(search(null, null), ADMIN);
        assertThat(adminResult.totalCount()).isEqualTo(3);
        assertThat(adminResult.items()).extracting(NoticeSummaryResponseDTO::isActive)
                .containsExactly(true, false, true);
    }

    @Test
    void searchRejectsAnotherRoleFilterForGeneralUser() {
        assertThatThrownBy(() -> noticeService.searchNotices(
                search(NoticeTargetRole.PROFESSOR, null),
                STUDENT
        )).isInstanceOf(NoticeAccessDeniedException.class);
    }

    @Test
    void detailAppliesRoleAndInactiveVisibilityRules() {
        NoticeDetailResponseDTO professorNotice = create("교수 공지", "내용", NoticeTargetRole.PROFESSOR);
        NoticeDetailResponseDTO studentNotice = create("학생 공지", "내용", NoticeTargetRole.STUDENT);
        noticeService.deleteNotice(studentNotice.id(), ADMIN, null, null);

        assertThatThrownBy(() -> noticeService.getNotice(professorNotice.id(), STUDENT))
                .isInstanceOf(NoticeAccessDeniedException.class);
        assertThatThrownBy(() -> noticeService.getNotice(studentNotice.id(), STUDENT))
                .isInstanceOf(NoticeNotFoundException.class);
        assertThat(noticeService.getNotice(studentNotice.id(), ADMIN).isActive()).isFalse();
    }

    @Test
    void updateEditsInactiveNoticeAndReactivatesIt() {
        NoticeDetailResponseDTO notice = create("삭제 전", "기존 본문", NoticeTargetRole.ALL);
        noticeService.deleteNotice(notice.id(), ADMIN, null, null);

        NoticeDetailResponseDTO edited = noticeService.updateNotice(
                notice.id(),
                new NoticeUpdateRequestDTO("  복구 공지  ", "  ", NoticeTargetRole.STUDENT, true),
                ADMIN,
                "notice-update-request",
                "127.0.0.2"
        );

        assertThat(edited.title()).isEqualTo("복구 공지");
        assertThat(edited.content()).isNull();
        assertThat(edited.targetRole()).isEqualTo(NoticeTargetRole.STUDENT);
        assertThat(edited.isActive()).isTrue();

        AuditLog updateLog = auditLogRepository.findAll().stream()
                .filter(log -> "NOTICE_UPDATE".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(updateLog.getBeforeValue()).containsEntry("isActive", false);
        assertThat(updateLog.getAfterValue()).containsEntry("isActive", true);
    }

    @Test
    void updateRejectsSameStateAndAllowsRepeatedContentOnReactivation() {
        NoticeDetailResponseDTO active = create("활성 공지", "내용", NoticeTargetRole.ALL);
        NoticeDetailResponseDTO inactive = create("복구 대상", "내용", NoticeTargetRole.STUDENT);
        noticeService.deleteNotice(inactive.id(), ADMIN, null, null);

        assertThatThrownBy(() -> noticeService.updateNotice(
                active.id(),
                new NoticeUpdateRequestDTO(null, null, null, true),
                ADMIN,
                null,
                null
        )).isInstanceOf(NoticeStateConflictException.class);

        create("재공지 대상", "동일", NoticeTargetRole.PROFESSOR);
        NoticeDetailResponseDTO reactivated = noticeService.updateNotice(
                inactive.id(),
                new NoticeUpdateRequestDTO("재공지 대상", "동일", NoticeTargetRole.PROFESSOR, true),
                ADMIN,
                null,
                null
        );

        assertThat(reactivated.isActive()).isTrue();
        assertThat(reactivated.title()).isEqualTo("재공지 대상");
    }

    @Test
    void updateRejectsEmptyPatchAndReturnsSameValueWithoutAudit() {
        NoticeDetailResponseDTO notice = create("동일 값", "내용", NoticeTargetRole.ALL);
        int initialAuditCount = auditLogRepository.findAll().size();

        assertThatThrownBy(() -> noticeService.updateNotice(
                notice.id(),
                new NoticeUpdateRequestDTO(null, null, null, null),
                ADMIN,
                null,
                null
        )).isInstanceOf(InvalidNoticeRequestException.class);

        NoticeDetailResponseDTO same = noticeService.updateNotice(
                notice.id(),
                new NoticeUpdateRequestDTO("동일 값", null, null, null),
                ADMIN,
                null,
                null
        );
        assertThat(same.id()).isEqualTo(notice.id());
        assertThat(auditLogRepository.findAll()).hasSize(initialAuditCount);
    }

    @Test
    void deleteDeactivatesAndRejectsRepeatedDelete() {
        NoticeDetailResponseDTO notice = create("삭제 공지", "내용", NoticeTargetRole.ALL);

        noticeService.deleteNotice(notice.id(), ADMIN, "notice-delete-request", "127.0.0.3");

        assertThat(noticeRepository.findById(notice.id())).get().extracting(Notice::isActive).isEqualTo(false);
        AuditLog deleteLog = auditLogRepository.findAll().stream()
                .filter(log -> "NOTICE_DELETE".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(deleteLog.getBeforeValue()).containsEntry("isActive", true);
        assertThat(deleteLog.getAfterValue()).containsEntry("isActive", false);

        assertThatThrownBy(() -> noticeService.deleteNotice(notice.id(), ADMIN, null, null))
                .isInstanceOf(NoticeStateConflictException.class);
    }

    @Test
    void mutationRejectsNonAdminEvenOutsideController() {
        assertThatThrownBy(() -> noticeService.createNotice(
                new NoticeCreateRequestDTO("학생 작성", "내용", NoticeTargetRole.ALL),
                STUDENT,
                null,
                null
        )).isInstanceOf(NoticeAccessDeniedException.class);
    }

    private NoticeDetailResponseDTO create(String title, String content, NoticeTargetRole targetRole) {
        return noticeService.createNotice(
                new NoticeCreateRequestDTO(title, content, targetRole),
                ADMIN,
                null,
                null
        );
    }

    private NoticeSearchRequestDTO search(NoticeTargetRole targetRole, Boolean active) {
        return new NoticeSearchRequestDTO(1, 20, null, targetRole, active);
    }
}
