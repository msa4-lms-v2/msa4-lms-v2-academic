package com.msa4lmsv2academic.domain.transfer.entity;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.msa4lmsv2academic.domain.organization.entity.Major;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "academic_change_requests", indexes = {
        @Index(name = "idx_academic_change_requests_student_status", columnList = "student_id,status"),
        @Index(name = "idx_academic_change_requests_status_created", columnList = "status,created_at"),
        @Index(name = "idx_academic_change_requests_target_semester", columnList = "target_semester_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_academic_change_requests_active_type",
        columnNames = {"request_type", "active_student_id"}))
public class AcademicChangeRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 20)
    private AcademicChangeRequestType requestType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_department_id", nullable = false)
    private Department sourceDepartment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_major_id")
    private Major sourceMajor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_department_id", nullable = false)
    private Department targetDepartment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_major_id", nullable = false)
    private Major targetMajor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_semester_id", nullable = false)
    private Semester targetSemester;
    @Column(nullable = false, length = 500)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AcademicChangeRequestStatus status;
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    @Column(name = "active_student_id", insertable = false, updatable = false)
    private Long activeStudentId;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = false)
    @BatchSize(size = 50)
    private List<AcademicChangeRequestFile> files = new ArrayList<>();
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AcademicChangeRequest create(Student student, Department targetDepartment, Major targetMajor,
                                                Semester targetSemester, String reason) {
        AcademicChangeRequest request = new AcademicChangeRequest();
        request.student = student;
        request.requestType = AcademicChangeRequestType.TRANSFER_DEPARTMENT;
        request.sourceDepartment = student.getDepartment();
        request.sourceMajor = student.getMajor();
        request.targetDepartment = targetDepartment;
        request.targetMajor = targetMajor;
        request.targetSemester = targetSemester;
        request.reason = reason;
        request.status = AcademicChangeRequestStatus.PENDING;
        return request;
    }

    public void addFile(AcademicChangeRequestFile file) {
        files.add(file);
    }

    public void approve(User processor, LocalDateTime now) {
        requirePending();
        status = AcademicChangeRequestStatus.APPROVED;
        processedBy = processor;
        processedAt = now;
    }

    public void reject(User processor, String reason, LocalDateTime now) {
        requirePending();
        status = AcademicChangeRequestStatus.REJECTED;
        rejectReason = reason;
        processedBy = processor;
        processedAt = now;
    }

    public void cancel(User actor, String reason, LocalDateTime now) {
        requirePending();
        status = AcademicChangeRequestStatus.CANCELLED;
        cancelReason = reason;
        cancelledBy = actor;
        cancelledAt = now;
    }

    private void requirePending() {
        if (status != AcademicChangeRequestStatus.PENDING) {
            throw new IllegalStateException("대기 중인 전과 신청만 처리할 수 있습니다.");
        }
    }
}
