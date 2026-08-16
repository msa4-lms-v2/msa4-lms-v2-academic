package com.msa4lmsv2academic.domain.infochange.entity;

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
        name = "student_info_change_requests",
        indexes = @Index(name = "idx_student_info_change_requests_student_status", columnList = "student_id, status")
)
public class StudentInfoChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private StudentInfoChangeRequest(
            Student student,
            String newName,
            String newPhoneNumber,
            String newEmail,
            String newAddress,
            String newProfileImageKey,
            String reason
    ) {
        this.student = student;
        this.newName = newName;
        this.newPhoneNumber = newPhoneNumber;
        this.newEmail = newEmail;
        this.newAddress = newAddress;
        this.newProfileImageKey = newProfileImageKey;
        this.reason = reason;
        this.status = InfoChangeRequestStatus.REQUESTED;
    }

    public static StudentInfoChangeRequest create(
            Student student,
            String newName,
            String newPhoneNumber,
            String newEmail,
            String newAddress,
            String newProfileImageKey,
            String reason
    ) {
        return new StudentInfoChangeRequest(
                student, newName, newPhoneNumber, newEmail, newAddress, newProfileImageKey, reason
        );
    }

    public void approve(User reviewer, LocalDateTime reviewedAt) {
        requireStatus(InfoChangeRequestStatus.REQUESTED);
        this.status = InfoChangeRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = null;
    }

    public void reject(User reviewer, String rejectReason, LocalDateTime reviewedAt) {
        requireStatus(InfoChangeRequestStatus.REQUESTED);
        this.status = InfoChangeRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

    private void requireStatus(InfoChangeRequestStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("허용되지 않은 학적 정보 변경 신청 상태 전이입니다.");
        }
    }
}
