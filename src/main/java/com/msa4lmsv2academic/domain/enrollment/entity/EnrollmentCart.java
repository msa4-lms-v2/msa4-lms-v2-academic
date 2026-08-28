package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "enrollment_carts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_carts_student_lecture",
                columnNames = {"student_id", "lecture_id"}
        ),
        indexes = {
                @Index(name = "idx_enrollment_carts_student_id", columnList = "student_id"),
                @Index(name = "idx_enrollment_carts_lecture_id", columnList = "lecture_id")
        }
)
public class EnrollmentCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private EnrollmentCart(Student student, Lecture lecture, LocalDateTime createdAt) {
        this.student = student;
        this.lecture = lecture;
        this.createdAt = createdAt;
    }

    public static EnrollmentCart create(Student student, Lecture lecture, LocalDateTime createdAt) {
        return new EnrollmentCart(student, lecture, createdAt);
    }
}
