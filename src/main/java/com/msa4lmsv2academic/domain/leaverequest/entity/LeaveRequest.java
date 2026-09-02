package com.msa4lmsv2academic.domain.leaverequest.entity;

import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "academic_requests")
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 20)
    private LeaveRequestType requestType;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "target_year", nullable = false)
    private short targetYear;
    @Column(name = "target_semester", nullable = false)
    private byte targetSemester;
    @Column(name = "return_year")
    private Short returnYear;
    @Column(name = "return_semester")
    private Byte returnSemester;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private LeaveRequestStatus status;
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;
    @Column(name = "attachment_stored_name", length = 255)
    private String attachmentStoredName;
    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;
    @Column(name = "attachment_size")
    private Long attachmentSize;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LeaveRequestFile> files = new ArrayList<>();
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static LeaveRequest create(Student student,
                                      LeaveRequestType type,
                                      String reason,
                                      short year,
                                      byte term,
                                      Short returnYear,
                                      Byte returnTerm)
    {
        LeaveRequest request = new LeaveRequest();
        request.student = student;
        request.requestType = type;
        request.reason = reason;
        request.targetYear = year;
        request.targetSemester = term;
        request.returnYear = returnYear;
        request.returnSemester = returnTerm;
        request.status = LeaveRequestStatus.PENDING;
        return request;
    }

    public void addFile(String originalName, String storedName, String contentType, long size) {
        files.add(LeaveRequestFile.create(this, originalName, storedName, contentType, size));
    }

    public void approve() {
        requirePending();
        status = LeaveRequestStatus.APPROVED;
    }

    public void reject(String reason) {
        requirePending();
        status = LeaveRequestStatus.REJECTED;
        rejectReason = reason;
    }

    public void cancel(String reason) {
        requirePending();
        status = LeaveRequestStatus.CANCELLED;
        cancelReason = reason;
    }

    private void requirePending() {
        if (status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("대기 중인 휴·복학 신청만 변경할 수 있습니다.");
        }
    }
}
