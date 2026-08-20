package com.msa4lmsv2academic.domain.infochange.entity;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
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
        name = "professor_info_change_requests",
        indexes = {
                @Index(name = "idx_professor_info_change_requests_professor_status", columnList = "professor_id, status"),
                @Index(name = "idx_professor_info_change_requests_status_created", columnList = "status, created_at")
        }
)
public class ProfessorInfoChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(name = "new_name", length = 50)
    private String newName;

    @Column(name = "new_phone_number", length = 20)
    private String newPhoneNumber;

    @Column(name = "new_email", length = 100)
    private String newEmail;

    @Column(name = "new_address", length = 255)
    private String newAddress;

    @Column(name = "new_profile_image_key", length = 500)
    private String newProfileImageKey;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InfoChangeRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ProfessorInfoChangeRequest(
            Professor professor,
            String newName,
            String newPhoneNumber,
            String newEmail,
            String newAddress,
            String newProfileImageKey,
            String reason
    ) {
        this.professor = professor;
        this.newName = newName;
        this.newPhoneNumber = newPhoneNumber;
        this.newEmail = newEmail;
        this.newAddress = newAddress;
        this.newProfileImageKey = newProfileImageKey;
        this.reason = reason;
        this.status = InfoChangeRequestStatus.REQUESTED;
    }

    public static ProfessorInfoChangeRequest create(
            Professor professor,
            String newName,
            String newPhoneNumber,
            String newEmail,
            String newAddress,
            String newProfileImageKey,
            String reason
    ) {
        return new ProfessorInfoChangeRequest(
                professor, newName, newPhoneNumber, newEmail, newAddress, newProfileImageKey, reason
        );
    }

    public void approve(User reviewer, LocalDateTime reviewedAt) {
        requireRequested();
        this.status = InfoChangeRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = null;
    }

    public void reject(User reviewer, String rejectReason, LocalDateTime reviewedAt) {
        requireRequested();
        this.status = InfoChangeRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

    public void cancel(LocalDateTime cancelledAt) {
        requireRequested();
        this.status = InfoChangeRequestStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    private void requireRequested() {
        if (status != InfoChangeRequestStatus.REQUESTED) {
            throw new IllegalStateException("처리 대기 상태인 교수 프로필 변경 신청만 처리할 수 있습니다.");
        }
    }
}
