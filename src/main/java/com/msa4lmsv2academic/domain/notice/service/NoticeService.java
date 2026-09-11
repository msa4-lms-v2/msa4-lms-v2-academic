package com.msa4lmsv2academic.domain.notice.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.infochange.service.ProfileFileValidator;
import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeAttachment;
import com.msa4lmsv2academic.domain.notice.entity.NoticeCategory;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
import com.msa4lmsv2academic.domain.notice.repository.NoticeAttachmentRepository;
import com.msa4lmsv2academic.domain.notice.repository.NoticeQueryRepository;
import com.msa4lmsv2academic.domain.notice.repository.NoticeRepository;
import com.msa4lmsv2academic.domain.notice.repository.NoticeSearchCondition;
import com.msa4lmsv2academic.domain.notice.repository.NoticeSearchResult;
import com.msa4lmsv2academic.domain.notice.request.NoticeCreateRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeSearchRequestDTO;
import com.msa4lmsv2academic.domain.notice.request.NoticeUpdateRequestDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeAttachmentResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeDetailResponseDTO;
import com.msa4lmsv2academic.domain.notice.response.NoticeSummaryResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.domain.user.repository.UserRepository;
import com.msa4lmsv2academic.global.error.InvalidNoticeRequestException;
import com.msa4lmsv2academic.global.error.NoticeAccessDeniedException;
import com.msa4lmsv2academic.global.error.NoticeAuthorNotFoundException;
import com.msa4lmsv2academic.global.error.NoticeNotFoundException;
import com.msa4lmsv2academic.global.error.NoticeStateConflictException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final String TARGET_TYPE = "NOTICE";
    private static final String CREATE_ACTION = "NOTICE_CREATE";
    private static final String UPDATE_ACTION = "NOTICE_UPDATE";
    private static final String DELETE_ACTION = "NOTICE_DELETE";
    private static final String ATTACHMENT_PATH_PREFIX = "notices/attachments";
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeRepository noticeRepository;
    private final NoticeQueryRepository noticeQueryRepository;
    private final NoticeAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ProfileFileValidator profileFileValidator;
    private final AuditLogService auditLogService;

    public PageResponseDTO<NoticeSummaryResponseDTO> searchNotices(NoticeSearchRequestDTO request, CurrentUser currentUser) {
        validateSearchRequest(request);
        NoticeTargetRole userRole = resolveUserRole(currentUser);
        boolean admin = currentUser.isAdmin();
        Set<NoticeTargetRole> targetRoles = resolveTargetRoles(request.targetRole(), userRole, admin);
        Boolean active = admin ? request.active() : Boolean.TRUE;
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        long offset = (page - 1L) * size;
        NoticeSearchResult result = noticeQueryRepository.search(new NoticeSearchCondition(
                offset, size, normalizeKeyword(request.keyword()), normalizeKeyword(request.authorKeyword()), request.category(),
                targetRoles, active, request.createdFrom(), request.createdTo()
        ));
        List<NoticeSummaryResponseDTO> items = result.items().stream().map(NoticeSummaryResponseDTO::from).toList();
        return new PageResponseDTO<>(items, result.totalCount(), page, size, offset + items.size() < result.totalCount());
    }

    public NoticeDetailResponseDTO getNotice(Long noticeId, CurrentUser currentUser) {
        Notice notice = findNotice(noticeId);
        if (currentUser == null) throw new NoticeAccessDeniedException();
        if (!currentUser.isAdmin()) {
            NoticeTargetRole userRole = resolveUserRole(currentUser);
            if (!notice.isActive()) throw new NoticeNotFoundException();
            if (notice.getTargetRole() != NoticeTargetRole.ALL && notice.getTargetRole() != userRole) {
                throw new NoticeAccessDeniedException();
            }
        }
        return toDetail(notice);
    }

    @Transactional
    public NoticeDetailResponseDTO createNotice(NoticeCreateRequestDTO request, CurrentUser currentUser,
                                                 String requestId, String ipAddress) {
        return createNotice(request, List.of(), currentUser, requestId, ipAddress);
    }

    @Transactional
    public NoticeDetailResponseDTO createNotice(NoticeCreateRequestDTO request, List<MultipartFile> attachments,
                                                 CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateCreateRequest(request);
        profileFileValidator.validateAttachments(attachments);
        NoticeCategory category = request.category() == null ? NoticeCategory.NORMAL : request.category();
        LocalDate normalTransitionDate = normalizeNormalTransitionDate(category, request.normalTransitionDate(), true);
        User author = userRepository.findById(currentUser.id()).orElseThrow(NoticeAuthorNotFoundException::new);
        Notice savedNotice = noticeRepository.saveAndFlush(Notice.create(
                normalizeTitle(request.title()), normalizeContent(request.content()), category, normalTransitionDate,
                request.targetRole(), author
        ));
        saveAttachments(savedNotice, attachments);
        auditLogService.record(currentUser.id(), CREATE_ACTION, TARGET_TYPE, savedNotice.getId(), null,
                snapshot(savedNotice), null, normalizeNullable(requestId), normalizeNullable(ipAddress));
        return toDetail(savedNotice);
    }

    @Transactional
    public NoticeDetailResponseDTO updateNotice(Long noticeId, NoticeUpdateRequestDTO request,
                                                 CurrentUser currentUser, String requestId, String ipAddress) {
        return updateNotice(noticeId, request, List.of(), currentUser, requestId, ipAddress);
    }

    @Transactional
    public NoticeDetailResponseDTO updateNotice(Long noticeId, NoticeUpdateRequestDTO request,
                                                 List<MultipartFile> attachments, CurrentUser currentUser,
                                                 String requestId, String ipAddress) {
        validateAdmin(currentUser);
        validateUpdateRequest(request);
        profileFileValidator.validateAttachments(attachmentRepository.countByNoticeId(noticeId), attachments);
        Notice notice = findNotice(noticeId);
        if (request.isActive() != null && request.isActive() == notice.isActive()) {
            throw new NoticeStateConflictException(notice.isActive());
        }
        String title = request.title() == null ? notice.getTitle() : normalizeTitle(request.title());
        String content = request.content() == null ? notice.getContent() : normalizeContent(request.content());
        NoticeCategory category = request.category() == null ? notice.getCategory() : request.category();
        LocalDate normalTransitionDate = resolveNormalTransitionDate(request, notice, category);
        NoticeTargetRole targetRole = request.targetRole() == null ? notice.getTargetRole() : request.targetRole();
        boolean active = request.isActive() == null ? notice.isActive() : request.isActive();
        boolean changed = !isSameNotice(notice, title, content, category, normalTransitionDate, targetRole, active);
        Map<String, Object> beforeValue = snapshot(notice);
        if (changed) notice.update(title, content, category, normalTransitionDate, targetRole, active);
        Notice savedNotice = noticeRepository.saveAndFlush(notice);
        saveAttachments(savedNotice, attachments);
        if (changed || hasPresentAttachment(attachments)) {
            auditLogService.record(currentUser.id(), UPDATE_ACTION, TARGET_TYPE, savedNotice.getId(), beforeValue,
                    snapshot(savedNotice), null, normalizeNullable(requestId), normalizeNullable(ipAddress));
        }
        return toDetail(savedNotice);
    }

    @Transactional
    public void deleteNotice(Long noticeId, CurrentUser currentUser, String requestId, String ipAddress) {
        validateAdmin(currentUser);
        Notice notice = findNotice(noticeId);
        if (!notice.isActive()) throw new NoticeStateConflictException(false);
        Map<String, Object> beforeValue = snapshot(notice);
        notice.deactivate();
        Notice savedNotice = noticeRepository.saveAndFlush(notice);
        auditLogService.record(currentUser.id(), DELETE_ACTION, TARGET_TYPE, savedNotice.getId(), beforeValue,
                snapshot(savedNotice), null, normalizeNullable(requestId), normalizeNullable(ipAddress));
    }

    @Transactional
    public void transitionExpiredImportantNotices(LocalDate now) {
        noticeRepository.findByCategoryAndNormalTransitionDateLessThanEqual(NoticeCategory.IMPORTANT, now)
                .forEach(Notice::transitionToNormal);
    }

    private void validateSearchRequest(NoticeSearchRequestDTO request) {
        if (request == null) throw new InvalidNoticeRequestException("공지사항 검색 조건이 필요합니다.");
        if (request.createdFrom() != null && request.createdTo() != null && request.createdFrom().isAfter(request.createdTo())) {
            throw new InvalidNoticeRequestException("작성 기간의 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private void validateCreateRequest(NoticeCreateRequestDTO request) {
        if (request == null || request.title() == null || request.title().isBlank() || request.targetRole() == null) {
            throw new InvalidNoticeRequestException("title과 targetRole은 필수입니다.");
        }
        if (request.title().strip().length() > 100) throw new InvalidNoticeRequestException("title은 100자 이하여야 합니다.");
        validateContentLength(request.content());
    }

    private void validateUpdateRequest(NoticeUpdateRequestDTO request) {
        if (request == null || !request.hasAnyUpdateField()) {
            throw new InvalidNoticeRequestException("수정할 공지사항 정보가 필요합니다.");
        }
        if (request.title() != null && (request.title().isBlank() || request.title().strip().length() > 100)) {
            throw new InvalidNoticeRequestException("title은 공백이 아닌 100자 이하의 값이어야 합니다.");
        }
        validateContentLength(request.content());
    }

    private void validateAdmin(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAdmin()) throw new NoticeAccessDeniedException();
    }

    private void validateContentLength(String content) {
        if (content != null && content.length() > 5000) {
            throw new InvalidNoticeRequestException("content는 5000자 이하여야 합니다.");
        }
    }

    private NoticeTargetRole resolveUserRole(CurrentUser currentUser) {
        if (currentUser == null || currentUser.role() == null || currentUser.isAdmin()) {
            if (currentUser != null && currentUser.isAdmin()) return null;
            throw new NoticeAccessDeniedException();
        }
        try {
            return NoticeTargetRole.valueOf(currentUser.role());
        } catch (IllegalArgumentException exception) {
            throw new NoticeAccessDeniedException();
        }
    }

    private Set<NoticeTargetRole> resolveTargetRoles(NoticeTargetRole requestedRole, NoticeTargetRole userRole, boolean admin) {
        if (admin) return requestedRole == null ? null : Set.of(requestedRole);
        if (requestedRole == null) return Set.of(NoticeTargetRole.ALL, userRole);
        if (requestedRole != NoticeTargetRole.ALL && requestedRole != userRole) throw new NoticeAccessDeniedException();
        return Set.of(requestedRole);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(NoticeNotFoundException::new);
    }

    private LocalDate resolveNormalTransitionDate(NoticeUpdateRequestDTO request, Notice notice, NoticeCategory category) {
        if (category == NoticeCategory.NORMAL) return normalizeNormalTransitionDate(category, request.normalTransitionDate(), false);
        LocalDate transitionDate = request.normalTransitionDate() == null
                ? notice.getNormalTransitionDate() : request.normalTransitionDate();
        return normalizeNormalTransitionDate(category, transitionDate, false);
    }

    private LocalDate normalizeNormalTransitionDate(NoticeCategory category, LocalDate transitionDate, boolean requireFuture) {
        if (category == NoticeCategory.NORMAL) {
            if (transitionDate != null) throw new InvalidNoticeRequestException("일반 공지에는 일반 공지 전환일을 설정할 수 없습니다.");
            return null;
        }
        if (transitionDate == null) throw new InvalidNoticeRequestException("중요 공지에는 일반 공지 전환일이 필요합니다.");
        if (requireFuture && !transitionDate.isAfter(LocalDate.now(KOREA_ZONE))) {
            throw new InvalidNoticeRequestException("일반 공지 전환일은 오늘 이후여야 합니다.");
        }
        return transitionDate;
    }

    private void saveAttachments(Notice notice, List<MultipartFile> attachments) {
        if (attachments == null) return;
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) continue;
            String objectKey = fileStorageService.upload(ATTACHMENT_PATH_PREFIX, attachment);
            attachmentRepository.save(NoticeAttachment.create(notice, attachment.getOriginalFilename(), objectKey,
                    attachment.getContentType(), attachment.getSize()));
        }
    }

    private NoticeDetailResponseDTO toDetail(Notice notice) {
        List<NoticeAttachmentResponseDTO> attachments = attachmentRepository.findByNoticeIdOrderByIdAsc(notice.getId())
                .stream().map(file -> NoticeAttachmentResponseDTO.from(
                        file, fileStorageService.presignedDownloadUrl(file.getObjectKey())
                )).toList();
        return NoticeDetailResponseDTO.from(notice, attachments);
    }

    private boolean hasPresentAttachment(List<MultipartFile> attachments) {
        return attachments != null && attachments.stream().anyMatch(file -> file != null && !file.isEmpty());
    }

    private boolean isSameNotice(Notice notice, String title, String content, NoticeCategory category,
                                 LocalDate transitionDate, NoticeTargetRole targetRole, boolean active) {
        return Objects.equals(notice.getTitle(), title) && Objects.equals(notice.getContent(), content)
                && notice.getCategory() == category && Objects.equals(notice.getNormalTransitionDate(), transitionDate)
                && notice.getTargetRole() == targetRole && notice.isActive() == active;
    }

    private String normalizeTitle(String title) { return title.strip(); }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.strip();
    }

    private String normalizeKeyword(String value) { return value == null || value.isBlank() ? null : value.strip(); }

    private String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value; }

    private Map<String, Object> snapshot(Notice notice) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", notice.getId());
        value.put("title", notice.getTitle());
        value.put("content", notice.getContent());
        value.put("category", notice.getCategory().name());
        value.put("normalTransitionDate", notice.getNormalTransitionDate() == null ? null : notice.getNormalTransitionDate().toString());
        value.put("targetRole", notice.getTargetRole().name());
        value.put("isActive", notice.isActive());
        value.put("createdAt", notice.getCreatedAt().toString());
        value.put("authorId", notice.getAuthor().getId());
        return value;
    }
}
