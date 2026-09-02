package com.msa4lmsv2academic.global.idempotency;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_keys_key", columnNames = "idempotency_key"),
        indexes = {
                @Index(name = "idx_idempotency_keys_requester", columnList = "requester_user_id"),
                @Index(name = "idx_idempotency_keys_expiry", columnList = "status,expires_at")
        })
public class AcademicIdempotencyKey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;
    @Column(nullable = false, length = 255)
    private String endpoint;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_snapshot", columnDefinition = "json")
    private String responseSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private IdempotencyStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static AcademicIdempotencyKey create(
            String key, Long userId, String endpoint, String requestHash, LocalDateTime now
    ) {
        AcademicIdempotencyKey entry = new AcademicIdempotencyKey();
        entry.idempotencyKey = key;
        entry.requesterUserId = userId;
        entry.endpoint = endpoint;
        entry.requestHash = requestHash;
        entry.status = IdempotencyStatus.IN_PROGRESS;
        entry.createdAt = now;
        entry.expiresAt = now.plusHours(24);
        return entry;
    }

    public boolean matches(Long userId, String endpoint, String requestHash) {
        return requesterUserId.equals(userId) && this.endpoint.equals(endpoint)
                && this.requestHash.equals(requestHash);
    }

    public void complete(String responseSnapshot) {
        this.responseSnapshot = responseSnapshot;
        this.status = IdempotencyStatus.COMPLETED;
    }
}
