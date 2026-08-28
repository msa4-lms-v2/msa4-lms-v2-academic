package com.msa4lmsv2academic.domain.dismissal.entity;

import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.DismissalConflictException;
import jakarta.persistence.*;
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
@Table(name = "dismissal_candidates")
public class DismissalCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @Version @Column(nullable = false)
    private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(name = "reason_type", nullable = false, length = 30)
    private DismissalReasonType reasonType;
    @Column(nullable = false, length = 500)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DismissalStatus status;
    @Column(name = "registered_by", nullable = false, updatable = false)
    private Long registeredBy;
    @Column(name = "processed_by")
    private Long processedBy;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DismissalCandidate create(Student student, DismissalReasonType type, String reason, Long actorId) {
        var candidate = new DismissalCandidate();
        candidate.student = student;
        candidate.reasonType = type;
        candidate.reason = reason;
        candidate.status = DismissalStatus.PENDING;
        candidate.registeredBy = actorId;
        return candidate;
    }

    public void revise(DismissalReasonType type, String reason) {
        requirePending();
        this.reasonType = type;
        this.reason = reason;
    }

    public void confirm(Long actorId, LocalDateTime now) {
        requirePending();
        status = DismissalStatus.CONFIRMED;
        processedBy = actorId;
        processedAt = now;
    }

    public void cancel(Long actorId, String reason, LocalDateTime now) {
        requirePending();
        status = DismissalStatus.CANCELLED;
        processedBy = actorId;
        processedAt = now;
        cancelReason = reason;
    }

    public void requirePending() {
        if (status != DismissalStatus.PENDING) {
            throw new DismissalConflictException("대기 중인 제적 후보만 변경할 수 있습니다.");
        }
    }
}
