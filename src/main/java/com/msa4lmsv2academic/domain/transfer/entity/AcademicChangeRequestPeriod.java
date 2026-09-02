package com.msa4lmsv2academic.domain.transfer.entity;

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
@Table(name = "academic_change_request_periods", uniqueConstraints =
        @UniqueConstraint(name = "uk_academic_change_periods_semester_type",
                columnNames = {"semester_id", "request_type"}))
public class AcademicChangeRequestPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 20)
    private AcademicChangeRequestType requestType;
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AcademicChangeRequestPeriod create(Semester semester, AcademicChangeRequestType requestType,
                                                       LocalDateTime startAt, LocalDateTime endAt, boolean active) {
        AcademicChangeRequestPeriod period = new AcademicChangeRequestPeriod();
        period.semester = semester;
        period.requestType = requestType;
        period.change(startAt, endAt, active);
        return period;
    }

    public void change(LocalDateTime startAt, LocalDateTime endAt, boolean active) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public boolean accepts(LocalDateTime now) {
        return active && !now.isBefore(startAt) && !now.isAfter(endAt);
    }
}
