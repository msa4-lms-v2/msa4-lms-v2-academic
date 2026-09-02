package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
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
@Table(
        name = "enrollment_credit_limit_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_credit_limit_rules_semester",
                columnNames = "semester_id"
        ),
        indexes = @Index(
                name = "idx_enrollment_credit_limit_rules_active_semester",
                columnList = "is_active,semester_id"
        ),
        check = @CheckConstraint(
                name = "ck_enrollment_credit_limit_rules_max_credits",
                constraint = "max_credits BETWEEN 1 AND 30"
        )
)
public class EnrollmentCreditLimitRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false, updatable = false)
    private Semester semester;

    @Column(name = "max_credits", nullable = false)
    private int maxCredits;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private EnrollmentCreditLimitRule(Semester semester, int maxCredits) {
        this.semester = semester;
        this.maxCredits = maxCredits;
        this.active = true;
    }

    public static EnrollmentCreditLimitRule create(Semester semester, int maxCredits) {
        return new EnrollmentCreditLimitRule(semester, maxCredits);
    }

    public void updateMaxCredits(int maxCredits) {
        this.maxCredits = maxCredits;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
