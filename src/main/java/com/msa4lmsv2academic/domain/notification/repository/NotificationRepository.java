package com.msa4lmsv2academic.domain.notification.repository;

import com.msa4lmsv2academic.domain.notification.entity.Notification;
import com.msa4lmsv2academic.domain.notification.entity.NotificationCategory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByDeduplicationKey(String deduplicationKey);
    Page<Notification> findByRecipientId(Long recipientUserId, Pageable pageable);
    Page<Notification> findByRecipientIdAndReadAtIsNull(Long recipientUserId, Pageable pageable);
    Page<Notification> findByRecipientIdAndCategory(Long recipientUserId, NotificationCategory category, Pageable pageable);
    Page<Notification> findByRecipientIdAndCategoryAndReadAtIsNull(Long recipientUserId, NotificationCategory category, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from Notification notification where notification.id = :notificationId "
            + "and notification.recipient.id = :recipientUserId")
    Optional<Notification> findOwnedByIdForUpdate(@Param("notificationId") Long notificationId,
                                                   @Param("recipientUserId") Long recipientUserId);
}
