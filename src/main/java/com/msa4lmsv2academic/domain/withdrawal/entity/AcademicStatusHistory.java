package com.msa4lmsv2academic.domain.withdrawal.entity;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
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
        name = "academic_status_histories",
        indexes = @Index(name = "idx_academic_status_histories_student_created", columnList = "student_id, created_at")
)
public class AcademicStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private AcademicStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private AcademicStatus newStatus;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AcademicStatusHistory(
            Student student,
            AcademicStatus previousStatus,
            AcademicStatus newStatus,
            String reason,
            User changedBy,
            String sourceType,
            Long sourceId
    ) {
        this.student = student;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    public static AcademicStatusHistory leaveChanged(
            Student student, AcademicStatus previousStatus, User changedBy, Long requestId
    ) {
        return new AcademicStatusHistory(student, previousStatus, student.getAcademicStatus(),
                student.getAcademicStatus() == AcademicStatus.ON_LEAVE ? "휴학 승인" : "복학 승인",
                changedBy, "LEAVE_REQUEST", requestId);
    }

    public static AcademicStatusHistory withdrawalApproved(
            Student student,
            AcademicStatus previousStatus,
            User changedBy,
            Long withdrawalId
    ) {
        return new AcademicStatusHistory(
                student,
                previousStatus,
                AcademicStatus.WITHDRAWN,
                "자퇴 최종 승인",
                changedBy,
                "WITHDRAWAL_REQUEST",
                withdrawalId
        );
    }
}
