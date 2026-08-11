package com.msa4lmsv2academic.domain.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_value", columnDefinition = "json")
    private Map<String, Object> beforeValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value", columnDefinition = "json")
    private Map<String, Object> afterValue;

    @Column(length = 255)
    private String reason;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AuditLog(Long actorId, String action, String targetType, Long targetId,
                     Map<String, Object> beforeValue, Map<String, Object> afterValue,
                     String reason, String requestId, String ipAddress) {
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
        this.requestId = requestId;
        this.ipAddress = ipAddress;
    }

    public static AuditLog create(Long actorId, String action, String targetType, Long targetId,
                                  Map<String, Object> beforeValue, Map<String, Object> afterValue,
                                  String reason, String requestId, String ipAddress) {
        return new AuditLog(actorId, action, targetType, targetId, beforeValue, afterValue,
                reason, requestId, ipAddress);
    }
}
