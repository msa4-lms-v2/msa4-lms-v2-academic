package com.msa4lmsv2academic.domain.gradeperiod.entity;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(
        name = "grade_operation_periods",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grade_operation_periods_semester_type",
                columnNames = {"semester_id", "operation_type"}
        )
)
public class GradeOperationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private GradeOperationType operationType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private GradeOperationPeriod(
            Semester semester,
            GradeOperationType operationType,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        if (semester == null || operationType == null) {
            throw new IllegalArgumentException("학기와 성적 처리 유형은 필수입니다.");
        }
        this.semester = semester;
        this.operationType = operationType;
        change(startDate, endDate, active);
    }

    public static GradeOperationPeriod create(
            Semester semester,
            GradeOperationType operationType,
            LocalDate startDate,
            LocalDate endDate,
            boolean active
    ) {
        return new GradeOperationPeriod(semester, operationType, startDate, endDate, active);
    }

    public void change(LocalDate startDate, LocalDate endDate, boolean active) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("성적 처리 시작일은 종료일보다 늦을 수 없습니다.");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public boolean accepts(LocalDate date) {
        return active && date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
