package com.msa4lmsv2academic.domain.attendance.entity;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
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
        name = "excuse_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_excuse_requests_enrollment_date_period",
                columnNames = {"enrollment_id", "lecture_date", "period"}
        ),
        indexes = {
                @Index(name = "idx_excuse_requests_enrollment_id", columnList = "enrollment_id"),
                @Index(name = "idx_excuse_requests_status", columnList = "status")
        }
)
public class ExcuseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;

    @Column(nullable = false)
    private byte period;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExcuseRequestStatus status;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    @Column(name = "attachment_stored_name", length = 255)
    private String attachmentStoredName;

    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ExcuseRequest(Enrollment enrollment, LocalDate lectureDate, byte period, String reason) {
        this.enrollment = enrollment;
        this.lectureDate = lectureDate;
        this.period = period;
        this.reason = reason;
        this.status = ExcuseRequestStatus.PENDING;
    }

    public static ExcuseRequest create(Enrollment enrollment, LocalDate lectureDate, byte period, String reason) {
        return new ExcuseRequest(enrollment, lectureDate, period, reason);
    }
}
