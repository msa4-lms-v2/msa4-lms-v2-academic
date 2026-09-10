package com.msa4lmsv2academic.domain.notification.entity;

import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(
        name = "uk_notifications_deduplication_key", columnNames = "deduplication_key"
), indexes = @Index(name = "idx_notifications_recipient_read_created", columnList = "recipient_user_id, read_at, created_at"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private NotificationCategory category;
    @Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;
    @Enumerated(EnumType.STRING) @Column(name = "resource_type", nullable = false, length = 40)
    private NotificationResourceType resourceType;
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(name = "deduplication_key", nullable = false, length = 64)
    private String deduplicationKey;
    @Column(name = "read_at")
    private LocalDateTime readAt;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(User recipient, NotificationCategory category, NotificationType type,
                         NotificationResourceType resourceType, Long resourceId, String title,
                         String message, String deduplicationKey) {
        this.recipient = recipient;
        this.category = category;
        this.type = type;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.title = title;
        this.message = message;
        this.deduplicationKey = deduplicationKey;
    }

    public static Notification create(User recipient, NotificationCategory category, NotificationType type,
                                      NotificationResourceType resourceType, Long resourceId, String title,
                                      String message, String deduplicationKey) {
        return new Notification(recipient, category, type, resourceType, resourceId, title, message, deduplicationKey);
    }

    public boolean isRead() { return readAt != null; }
    public void markRead(LocalDateTime now) { if (readAt == null) readAt = now; }
}
