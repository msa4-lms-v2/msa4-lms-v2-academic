package com.msa4lmsv2academic.domain.counseling.entity;

import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "counseling_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_counseling_notifications_deduplication_key",
                columnNames = "deduplication_key"
        ),
        indexes = {
                @Index(
                        name = "idx_counseling_notifications_recipient_read_created",
                        columnList = "recipient_user_id, read_at, created_at"
                ),
                @Index(name = "idx_counseling_notifications_counseling", columnList = "counseling_id")
        }
)
public class CounselingNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counseling_id", nullable = false)
    private Counseling counseling;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private CounselingNotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private CounselingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private CounselingStatus newStatus;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "deduplication_key", nullable = false, length = 64)
    private String deduplicationKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CounselingNotification(
            Counseling counseling,
            User recipient,
            CounselingNotificationType type,
            CounselingStatus previousStatus,
            CounselingStatus newStatus,
            String message,
            String deduplicationKey
    ) {
        this.counseling = counseling;
        this.recipient = recipient;
        this.type = type;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.message = message;
        this.deduplicationKey = deduplicationKey;
    }

    public static CounselingNotification create(
            Counseling counseling,
            User recipient,
            CounselingNotificationType type,
            CounselingStatus previousStatus,
            CounselingStatus newStatus,
            String message,
            String deduplicationKey
    ) {
        return new CounselingNotification(
                counseling,
                recipient,
                type,
                previousStatus,
                newStatus,
                message,
                deduplicationKey
        );
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }
}
