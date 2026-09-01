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
@Table(
        name = "attendances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendances_session_enrollment",
                columnNames = {"session_id", "enrollment_id"}
        ),
        indexes = {
                @Index(name = "idx_attendances_session_id", columnList = "session_id"),
                @Index(name = "idx_attendances_enrollment_id", columnList = "enrollment_id")
        }
)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;

    @Column(nullable = false)
    private Integer period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(length = 255)
    private String remarks;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "is_modified", nullable = false)
    private boolean modified;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Attendance(
            Enrollment enrollment,
            AttendanceSession session,
            AttendanceStatus status,
            String remarks,
            LocalDateTime checkInTime
    ) {
        this.enrollment = enrollment;
        this.session = session;
        this.lectureDate = session.getSessionDate();
        this.period = session.getPeriod();
        this.status = status;
        this.remarks = remarks;
        this.checkInTime = checkInTime;
        this.modified = false;
    }

    public static Attendance checkIn(
            Enrollment enrollment,
            AttendanceSession session,
            LocalDateTime checkInTime
    ) {
        return new Attendance(
                enrollment,
                session,
                AttendanceStatus.PRESENT,
                null,
                checkInTime
        );
    }

    public static Attendance record(
            Enrollment enrollment,
            AttendanceSession session,
            AttendanceStatus status,
            String remarks
    ) {
        return new Attendance(enrollment, session, status, remarks, null);
    }

    public void modify(AttendanceStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
        this.modified = true;
    }
}
