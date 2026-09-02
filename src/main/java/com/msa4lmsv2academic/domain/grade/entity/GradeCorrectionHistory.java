package com.msa4lmsv2academic.domain.grade.entity;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "grade_correction_histories")
public class GradeCorrectionHistory {

    public static final String RETAKE_REFLECTION_FIELD = "RETAKE_REFLECTION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "field_changed", nullable = false, length = 30)
    private String fieldChanged;

    @Column(name = "previous_value", length = 50)
    private String previousValue;

    @Column(name = "new_value", length = 50)
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private GradeCorrectionHistory(
            Enrollment enrollment,
            String fieldChanged,
            String previousValue,
            String newValue,
            User changedBy,
            String reason,
            LocalDateTime createdAt
    ) {
        this.enrollment = enrollment;
        this.fieldChanged = fieldChanged;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public static GradeCorrectionHistory recordRetakeReflection(
            Enrollment reflectedEnrollment,
            Enrollment previousEnrollment,
            User changedBy,
            String reason,
            LocalDateTime createdAt
    ) {
        return new GradeCorrectionHistory(
                reflectedEnrollment,
                RETAKE_REFLECTION_FIELD,
                gradeValue(previousEnrollment),
                gradeValue(reflectedEnrollment),
                changedBy,
                reason,
                createdAt
        );
    }

    public static String gradeValue(Enrollment enrollment) {
        return enrollment.getId() + ":" + enrollment.getLetterGrade();
    }
}
