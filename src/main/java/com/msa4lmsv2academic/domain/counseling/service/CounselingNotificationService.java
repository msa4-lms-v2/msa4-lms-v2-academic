package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotification;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingNotificationType;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.counseling.event.CounselingNotificationCreatedEvent;
import com.msa4lmsv2academic.domain.counseling.repository.CounselingNotificationRepository;
import com.msa4lmsv2academic.domain.counseling.request.CounselingNotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.counseling.response.CounselingNotificationResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.CounselingAccessDeniedException;
import com.msa4lmsv2academic.global.error.CounselingNotificationNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCounselingRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingNotificationService {
    private final CounselingNotificationRepository repository;
    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createRequested(Counseling counseling) {
        create(counseling, counseling.getProfessor().getUser(),
                CounselingNotificationType.COUNSELING_REQUESTED, null, CounselingStatus.WAITING,
                "새로운 온라인 상담이 신청되었습니다.", counseling.getQuestion());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createAnswered(Counseling counseling, CounselingStatus previous, boolean updated) {
        create(counseling, counseling.getStudent().getUser(),
                updated ? CounselingNotificationType.COUNSELING_ANSWER_UPDATED
                        : CounselingNotificationType.COUNSELING_ANSWERED,
                previous, CounselingStatus.ANSWERED,
                updated ? "교수 상담 답변이 수정되었습니다." : "교수 상담 답변이 등록되었습니다.",
                counseling.getAnswer());
    }

    private void create(Counseling counseling, User recipient, CounselingNotificationType type,
                        CounselingStatus previous, CounselingStatus next, String message, String content) {
        String key = digest(counseling.getId(), recipient.getId(), type, previous, next, content);
        if (repository.existsByDeduplicationKey(key)) return;
        CounselingNotification saved = repository.saveAndFlush(CounselingNotification.create(
                counseling, recipient, type, previous, next, message, key));
        publisher.publishEvent(new CounselingNotificationCreatedEvent(
                recipient.getId(), CounselingNotificationResponseDTO.from(saved)));
    }

    public PageResponseDTO<CounselingNotificationResponseDTO> search(
            CounselingNotificationSearchRequestDTO request, CurrentUser user) {
        requireParticipant(user);
        int page = request.resolvedPage();
        int size = request.resolvedSize();
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<CounselingNotification> result = request.resolvedUnreadOnly()
                ? repository.findByRecipientIdAndReadAtIsNull(user.id(), pageable)
                : repository.findByRecipientId(user.id(), pageable);
        return new PageResponseDTO<>(result.map(CounselingNotificationResponseDTO::from).getContent(),
                result.getTotalElements(), page, size, result.hasNext());
    }

    @Transactional
    public CounselingNotificationResponseDTO markRead(Long id, CurrentUser user) {
        requireParticipant(user);
        if (id == null || id <= 0) {
            throw new InvalidCounselingRequestException("notificationId는 양수여야 합니다.");
        }
        CounselingNotification notification = repository.findOwnedByIdForUpdate(id, user.id())
                .orElseThrow(CounselingNotificationNotFoundException::new);
        notification.markRead(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return CounselingNotificationResponseDTO.from(repository.saveAndFlush(notification));
    }

    private String digest(Long counselingId, Long recipientId, CounselingNotificationType type,
                          CounselingStatus previous, CounselingStatus next, String content) {
        String source = String.join("|", Objects.toString(counselingId, ""),
                Objects.toString(recipientId, ""), type.name(), Objects.toString(previous, ""),
                next.name(), Objects.toString(content, ""));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void requireParticipant(CurrentUser user) {
        if (user == null || user.id() == null || !Set.of("STUDENT", "PROFESSOR").contains(user.role())) {
            throw new CounselingAccessDeniedException("상담 참여자만 알림을 사용할 수 있습니다.");
        }
    }
}
