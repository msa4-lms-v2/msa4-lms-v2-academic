package com.msa4lmsv2academic.domain.coursecorrection.entity;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course_correction_periods", uniqueConstraints =
        @UniqueConstraint(name = "uk_course_correction_periods_semester", columnNames = "semester_id"))
public class CourseCorrectionPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CourseCorrectionPeriod(Semester semester, LocalDate startAt, LocalDate endAt, boolean active) {
        this.semester = semester;
        change(startAt, endAt, active);
    }

    public static CourseCorrectionPeriod create(Semester semester, LocalDate startAt, LocalDate endAt,
                                                boolean active) {
        return new CourseCorrectionPeriod(semester, startAt, endAt, active);
    }

    public void change(LocalDate startAt, LocalDate endAt, boolean active) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    public boolean accepts(LocalDate date) {
        return active && !date.isBefore(startAt) && !date.isAfter(endAt);
    }
}
