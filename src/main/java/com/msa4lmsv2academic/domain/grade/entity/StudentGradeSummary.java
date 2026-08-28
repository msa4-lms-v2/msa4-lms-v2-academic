package com.msa4lmsv2academic.domain.grade.entity;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.Student;
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
import java.math.BigDecimal;
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
        name = "student_grade_summaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_grade_summaries_student_semester",
                columnNames = {"student_id", "semester_id"}
        )
)
public class StudentGradeSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "total_credits", nullable = false)
    private short totalCredits;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal gpa;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private StudentGradeSummary(Student student, Semester semester, short totalCredits, BigDecimal gpa) {
        this.student = student;
        this.semester = semester;
        this.totalCredits = totalCredits;
        this.gpa = gpa;
    }

    public static StudentGradeSummary create(
            Student student,
            Semester semester,
            short totalCredits,
            BigDecimal gpa
    ) {
        return new StudentGradeSummary(student, semester, totalCredits, gpa);
    }

    public void update(short totalCredits, BigDecimal gpa) {
        this.totalCredits = totalCredits;
        this.gpa = gpa;
    }
}
