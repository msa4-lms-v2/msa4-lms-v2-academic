package com.msa4lmsv2academic.domain.notice.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import com.msa4lmsv2academic.domain.notice.repository.NoticeQueryRepository;
import com.msa4lmsv2academic.domain.notice.repository.NoticeRepository;
import com.msa4lmsv2academic.domain.notice.repository.NoticeSearchCondition;
import com.msa4lmsv2academic.domain.notice.repository.NoticeSearchResult;
import com.msa4lmsv2academic.domain.notice.request.NoticeCreateRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeSearchRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeUpdateRequestDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeDetailResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.InvalidNoticeRequestException;
import com.msa4lmsv2academic.global.error.NoticeAccessDeniedException;
import com.msa4lmsv2academic.global.error.NoticeAuthorNotFoundException;
import com.msa4lmsv2academic.global.error.NoticeNotFoundException;
import com.msa4lmsv2academic.global.error.NoticeStateConflictException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final String TARGET_TYPE = "NOTICE";
    private static final String CREATE_ACTION = "NOTICE_CREATE";
    private static final String UPDATE_ACTION = "NOTICE_UPDATE";
    private static final String DELETE_ACTION = "NOTICE_DELETE";

    private final NoticeRepository noticeRepository;
    private final NoticeQueryRepository noticeQueryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public PageResponseDTO<NoticeSummaryResponseDTO> searchNotices(NoticeSearchRequestDTO request, CurrentUser currentUser) {
        validateSearchRequest(request);
        NoticeTargetRole userRole = resolveUserRole(currentUser);
        boolean admin = currentUser.isAdmin();
        Set<NoticeTargetRole> targetRoles = resolveTargetRoles(request.targetRole(), userRole, admin);
        Boolean active;
        if (admin) {
            active = request.active();
        } else {
            active = Boolean.TRUE;
        }

        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        NoticeSearchCondition condition = new NoticeSearchCondition(
                offset,
                size,
                normalizeKeyword(request.keyword()),
                targetRoles,
                active
        );

        NoticeSearchResult result = noticeQueryRepository.search(condition);
        List<NoticeSummaryResponseDTO> items = result.items().stream()
                .map(NoticeSummaryResponseDTO::from)
                .toList();
        boolean hasNext = offset + items.size() < result.totalCount();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, hasNext);
    }

    public NoticeDetailResponseDTO getNotice(Long noticeId, CurrentUser currentUser) {
        Notice notice = findNotice(noticeId);
        if (currentUser == null) {
            throw new NoticeAccessDeniedException();
        }
        if (currentUser.isAdmin()) {
            return NoticeDetailResponseDTO.from(notice);
        }

        NoticeTargetRole userRole = resolveUserRole(currentUser);
        if (!notice.isActive()) {
            throw new NoticeNotFoundException();
        }
        if (notice.getTargetRole() != NoticeTargetRole.ALL && notice.getTargetRole() != userRole) {
            throw new NoticeAccessDeniedException();
        }
        return NoticeDetailResponseDTO.from(notice);
    }

    @Transactional
    public NoticeDetailResponseDTO createNotice(NoticeCreateRequestDTO request, CurrentUser currentUser,
                                                 String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateCreateRequest(request);

        String title = normalizeTitle(request.title());
        String content = normalizeContent(request.content());

        User author = userRepository.findById(currentUser.id())
                .orElseThrow(NoticeAuthorNotFoundException::new);
        Notice savedNotice = noticeRepository.saveAndFlush(
                Notice.create(title, content, request.targetRole(), author)
        );

        auditLogService.record(
                currentUser.id(),
                CREATE_ACTION,
                TARGET_TYPE,
                savedNotice.getId(),
                null,
                snapshot(savedNotice),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return NoticeDetailResponseDTO.from(savedNotice);
    }

    @Transactional
    public NoticeDetailResponseDTO updateNotice(Long noticeId, NoticeUpdateRequestDTO request,
                                                 CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);

        Notice notice = findNotice(noticeId);
        if (request.isActive() != null && request.isActive() == notice.isActive()) {
            throw new NoticeStateConflictException(notice.isActive());
        }

        String targetTitle = request.title() == null ? notice.getTitle() : normalizeTitle(request.title());
        String targetContent = request.content() == null ? notice.getContent() : normalizeContent(request.content());
        NoticeTargetRole targetRole = request.targetRole() == null ? notice.getTargetRole() : request.targetRole();
        boolean targetActive = request.isActive() == null ? notice.isActive() : request.isActive();

        if (isSameNotice(notice, targetTitle, targetContent, targetRole, targetActive)) {
            return NoticeDetailResponseDTO.from(notice);
        }
        Map<String, Object> beforeValue = snapshot(notice);
        notice.update(targetTitle, targetContent, targetRole, targetActive);
        Notice savedNotice = noticeRepository.saveAndFlush(notice);
        auditLogService.record(
                currentUser.id(),
                UPDATE_ACTION,
                TARGET_TYPE,
                savedNotice.getId(),
                beforeValue,
                snapshot(savedNotice),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
        return NoticeDetailResponseDTO.from(savedNotice);
    }

    @Transactional
    public void deleteNotice(Long noticeId, CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        Notice notice = findNotice(noticeId);
        if (!notice.isActive()) {
            throw new NoticeStateConflictException(false);
        }

        Map<String, Object> beforeValue = snapshot(notice);
        notice.deactivate();
        Notice savedNotice = noticeRepository.saveAndFlush(notice);
        auditLogService.record(
                currentUser.id(),
                DELETE_ACTION,
                TARGET_TYPE,
                savedNotice.getId(),
                beforeValue,
                snapshot(savedNotice),
                null,
                normalizeNullable(requestId),
                normalizeNullable(ipAddress)
        );
    }

    private void validateSearchRequest(NoticeSearchRequestDTO request) {
        if (request == null) {
            throw new InvalidNoticeRequestException("공지사항 검색 조건이 필요합니다.");
        }
    }

    private void validateCreateRequest(NoticeCreateRequestDTO request) {
        if (request == null || request.title() == null || request.title().isBlank() || request.targetRole() == null) {
            throw new InvalidNoticeRequestException("title과 targetRole은 필수입니다.");
        }
        if (request.title().strip().length() > 100) {
            throw new InvalidNoticeRequestException("title은 100자 이하여야 합니다.");
        }
        validateContentLength(request.content());
    }

    private void validateUpdateRequest(NoticeUpdateRequestDTO request) {
        if (request == null || !request.hasAnyUpdateField()) {
            throw new InvalidNoticeRequestException("title, content, targetRole, isActive 중 최소 한 필드가 필요합니다.");
        }
        if (request.title() != null && (request.title().isBlank() || request.title().strip().length() > 100)) {
            throw new InvalidNoticeRequestException("title은 공백이 아닌 100자 이하의 값이어야 합니다.");
        }
        validateContentLength(request.content());
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new NoticeAccessDeniedException();
        }
    }

    private void validateContentLength(String content) {
        if (content != null && content.length() > 5000) {
            throw new InvalidNoticeRequestException("content는 5000자 이하여야 합니다.");
        }
    }

    private NoticeTargetRole resolveUserRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() == null || currentUser.isAdmin()) {
            if (currentUser != null && currentUser.isAdmin()) {
                return null;
            }
            throw new NoticeAccessDeniedException();
        }
        try {
            return NoticeTargetRole.valueOf(currentUser.role());
        } catch (IllegalArgumentException exception) {
            throw new NoticeAccessDeniedException();
        }
    }

    private Set<NoticeTargetRole> resolveTargetRoles(NoticeTargetRole requestedRole, NoticeTargetRole userRole,
                                                     boolean admin) {
        if (admin) {
            return requestedRole == null ? null : Set.of(requestedRole);
        }
        if (requestedRole == null) {
            return Set.of(NoticeTargetRole.ALL, userRole);
        }
        if (requestedRole != NoticeTargetRole.ALL && requestedRole != userRole) {
            throw new NoticeAccessDeniedException();
        }
        return Set.of(requestedRole);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }

    private boolean isSameNotice(Notice notice, String title, String content, NoticeTargetRole targetRole,
                                 boolean active) {
        return Objects.equals(notice.getTitle(), title)
                && Objects.equals(notice.getContent(), content)
                && notice.getTargetRole() == targetRole
                && notice.isActive() == active;
    }

    private String normalizeTitle(String title) {
        return title.strip();
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.strip();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Map<String, Object> snapshot(Notice notice) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", notice.getId());
        value.put("title", notice.getTitle());
        value.put("content", notice.getContent());
        value.put("targetRole", notice.getTargetRole().name());
        value.put("isActive", notice.isActive());
        value.put("createdAt", notice.getCreatedAt().toString());
        value.put("authorId", notice.getAuthor().getId());
        return value;
    }
}
