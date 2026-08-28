package com.msa4lmsv2academic.domain.leaverequest.entity;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
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
@Table(name = "leave_request_periods", uniqueConstraints =
        @UniqueConstraint(name = "uk_leave_request_periods_semester_type", columnNames = {"semester_id", "request_type"}))
public class LeaveRequestPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 20)
    private LeaveRequestType requestType;
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;
    @Column(name = "approval_start_at", nullable = false)
    private LocalDateTime approvalStartAt;
    @Column(name = "approval_end_at", nullable = false)
    private LocalDateTime approvalEndAt;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static LeaveRequestPeriod create(Semester semester, LeaveRequestType type,
                                            LocalDateTime start, LocalDateTime end,
                                            LocalDateTime approvalStart, LocalDateTime approvalEnd, boolean active) {
        LeaveRequestPeriod period = new LeaveRequestPeriod();
        period.semester = semester;
        period.requestType = type;
        period.change(start, end, approvalStart, approvalEnd, active);
        return period;
    }

    public void change(LocalDateTime start, LocalDateTime end,
                       LocalDateTime approvalStart, LocalDateTime approvalEnd, boolean active) {
        this.startAt = start;
        this.endAt = end;
        this.approvalStartAt = approvalStart;
        this.approvalEndAt = approvalEnd;
        this.active = active;
    }

    public boolean accepts(LocalDateTime now) {
        return active && !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public boolean allowsApproval(LocalDateTime now) {
        return active && !now.isBefore(approvalStartAt) && !now.isAfter(approvalEndAt);
    }
}
