package com.msa4lmsv2academic.domain.withdrawal.entity;

import com.msa4lmsv2academic.domain.student.entity.Student;
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
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "withdrawal_requests",
        indexes = {
                @Index(name = "idx_withdrawal_requests_student_status", columnList = "student_id, status"),
                @Index(name = "idx_withdrawal_requests_status_created", columnList = "status, created_at")
        }
)
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "requested_effective_date")
    private LocalDate requestedEffectiveDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WithdrawalStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advisor_reviewed_by")
    private User advisorReviewedBy;

    @Column(name = "advisor_reviewed_at")
    private LocalDateTime advisorReviewedAt;

    @Column(name = "advisor_reject_reason", length = 500)
    private String advisorRejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private WithdrawalRequest(
            Student student,
            String reason,
            LocalDate requestedEffectiveDate,
            User requestedBy
    ) {
        this.student = student;
        this.reason = reason;
        this.requestedEffectiveDate = requestedEffectiveDate;
        this.requestedBy = requestedBy;
        this.status = WithdrawalStatus.PENDING;
    }

    public static WithdrawalRequest create(
            Student student,
            String reason,
            LocalDate requestedEffectiveDate,
            User requestedBy
    ) {
        return new WithdrawalRequest(student, reason, requestedEffectiveDate, requestedBy);
    }

    public void advisorApprove(User reviewer, LocalDateTime reviewedAt) {
        requireStatus(WithdrawalStatus.PENDING);
        this.status = WithdrawalStatus.ADVISOR_APPROVED;
        this.advisorReviewedBy = reviewer;
        this.advisorReviewedAt = reviewedAt;
        this.advisorRejectReason = null;
    }

    public void advisorReject(User reviewer, String rejectReason, LocalDateTime reviewedAt) {
        requireStatus(WithdrawalStatus.PENDING);
        this.status = WithdrawalStatus.ADVISOR_REJECTED;
        this.advisorReviewedBy = reviewer;
        this.advisorReviewedAt = reviewedAt;
        this.advisorRejectReason = rejectReason;
    }

    public void approve(User processor, LocalDate effectiveDate, LocalDateTime processedAt) {
        requireStatus(WithdrawalStatus.ADVISOR_APPROVED);
        this.status = WithdrawalStatus.APPROVED;
        this.processedBy = processor;
        this.effectiveDate = effectiveDate;
        this.processedAt = processedAt;
        this.rejectReason = null;
    }

    public void reject(User processor, String rejectReason, LocalDateTime processedAt) {
        requireStatus(WithdrawalStatus.ADVISOR_APPROVED);
        this.status = WithdrawalStatus.REJECTED;
        this.processedBy = processor;
        this.processedAt = processedAt;
        this.rejectReason = rejectReason;
    }

    public void cancel(User actor, String reason, LocalDateTime cancelledAt) {
        if (status != WithdrawalStatus.PENDING && status != WithdrawalStatus.ADVISOR_APPROVED) {
            throw new IllegalStateException("진행 중인 자퇴 신청만 취소할 수 있습니다.");
        }
        this.status = WithdrawalStatus.CANCELLED;
        this.cancelledBy = actor;
        this.cancelReason = reason;
        this.cancelledAt = cancelledAt;
    }

    private void requireStatus(WithdrawalStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("허용되지 않은 자퇴 승인 상태 전이입니다.");
        }
    }
}
