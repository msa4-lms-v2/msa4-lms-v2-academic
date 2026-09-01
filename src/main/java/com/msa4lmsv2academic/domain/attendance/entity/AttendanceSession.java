package com.msa4lmsv2academic.domain.attendance.entity;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attendance_sessions_lecture_date_period",
                        columnNames = {
                                "lecture_id",
                                "session_date",
                                "period"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private Integer period;

    // Auth users.id의 논리적 참조
    @Column(name = "opened_by", nullable = false)
    private Long openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceSessionStatus status;

    public void close(LocalDateTime now) {
        if (status == AttendanceSessionStatus.CLOSED) {
            throw new IllegalStateException("이미 종료된 출석 세션입니다.");
        }

        status = AttendanceSessionStatus.CLOSED;
        closedAt = now;
    }

    public void reopen(LocalDateTime now, Long openedBy) {
        if (status == AttendanceSessionStatus.OPEN) {
            throw new IllegalStateException("이미 진행 중인 출석 세션입니다.");
        }

        status = AttendanceSessionStatus.OPEN;
        this.openedBy = openedBy;
        openedAt = now;
        closedAt = null;
    }


    // 출석 세션 정보를 담은 Java 객체를 생성하는 코드
    private AttendanceSession(
            Lecture lecture,
            LocalDate sessionDate,
            Integer period,
            Long openedBy,
            LocalDateTime openedAt
    ) {
        this.lecture = lecture;
        this.sessionDate = sessionDate;
        this.period = period;
        this.openedBy = openedBy;
        this.openedAt = openedAt;
        this.status = AttendanceSessionStatus.OPEN;
    }

    public static AttendanceSession open(
            Lecture lecture,
            LocalDate sessionDate,
            Integer period,
            Long openedBy,
            LocalDateTime openedAt
    ) {
        return new AttendanceSession(
                lecture,
                sessionDate,
                period,
                openedBy,
                openedAt
        );
    }
}
