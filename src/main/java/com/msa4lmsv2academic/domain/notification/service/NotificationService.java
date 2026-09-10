package com.msa4lmsv2academic.domain.notification.service;

import com.msa4lmsv2academic.domain.notification.entity.*;
import com.msa4lmsv2academic.domain.notification.event.NotificationCreatedEvent;
import com.msa4lmsv2academic.domain.notification.repository.NotificationRepository;
import com.msa4lmsv2academic.domain.notification.request.NotificationSearchRequestDTO;
import com.msa4lmsv2academic.domain.notification.response.NotificationResponseDTO;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.msa4lmsv2academic.global.error.InvalidNotificationRequestException;
import com.msa4lmsv2academic.global.error.NotificationAccessDeniedException;
import com.msa4lmsv2academic.global.error.NotificationNotFoundException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
public class NotificationService {
    private final NotificationRepository repository;
    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(User recipient, NotificationCategory category, NotificationType type,
                       NotificationResourceType resourceType, Long resourceId, String title,
                       String message, String deduplicationKey) {
        if (repository.existsByDeduplicationKey(deduplicationKey)) return;
        Notification saved = repository.saveAndFlush(Notification.create(recipient, category, type, resourceType,
                resourceId, title, message, deduplicationKey));
        publisher.publishEvent(new NotificationCreatedEvent(recipient.getId(), NotificationResponseDTO.from(saved)));
    }

    public PageResponseDTO<NotificationResponseDTO> search(NotificationSearchRequestDTO request, CurrentUser user) {
        requireRecipient(user);
        return search(request.resolvedPage(), request.resolvedSize(), request.category(), request.resolvedUnreadOnly(), user.id());
    }

    public PageResponseDTO<NotificationResponseDTO> searchCounseling(int page, int size, boolean unreadOnly, CurrentUser user) {
        requireCounselingParticipant(user);
        return search(page, size, NotificationCategory.COUNSELING, unreadOnly, user.id());
    }

    @Transactional
    public NotificationResponseDTO markRead(Long id, CurrentUser user) {
        requireRecipient(user);
        if (id == null || id <= 0) throw new InvalidNotificationRequestException("notificationId는 양수여야 합니다.");
        Notification notification = repository.findOwnedByIdForUpdate(id, user.id()).orElseThrow(NotificationNotFoundException::new);
        notification.markRead(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return NotificationResponseDTO.from(repository.saveAndFlush(notification));
    }

    private PageResponseDTO<NotificationResponseDTO> search(int page, int size, NotificationCategory category,
                                                              boolean unreadOnly, Long recipientUserId) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> result;
        if (category == null) {
            result = unreadOnly ? repository.findByRecipientIdAndReadAtIsNull(recipientUserId, pageable)
                    : repository.findByRecipientId(recipientUserId, pageable);
        } else {
            result = unreadOnly ? repository.findByRecipientIdAndCategoryAndReadAtIsNull(recipientUserId, category, pageable)
                    : repository.findByRecipientIdAndCategory(recipientUserId, category, pageable);
        }
        return new PageResponseDTO<>(result.map(NotificationResponseDTO::from).getContent(), result.getTotalElements(),
                page, size, result.hasNext());
    }

    private void requireRecipient(CurrentUser user) {
        if (user == null || user.id() == null || !Set.of("STUDENT", "PROFESSOR", "ADMIN").contains(user.role())) {
            throw new NotificationAccessDeniedException();
        }
    }

    private void requireCounselingParticipant(CurrentUser user) {
        if (user == null || user.id() == null || !Set.of("STUDENT", "PROFESSOR").contains(user.role())) {
            throw new NotificationAccessDeniedException();
        }
    }
}
