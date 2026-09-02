package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "enrollment_histories", indexes = {
        @Index(name = "idx_enrollment_histories_student_id", columnList = "student_id"),
        @Index(name = "idx_enrollment_histories_lecture_id", columnList = "lecture_id")
})
public class EnrollmentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EnrollmentHistoryAction action;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static EnrollmentHistory from(Enrollment enrollment) {
        return create(enrollment, EnrollmentHistoryAction.ENROLL, enrollment.getEnrolledAt());
    }

    public static EnrollmentHistory fromCancellation(Enrollment enrollment, LocalDateTime cancelledAt) {
        return create(enrollment, EnrollmentHistoryAction.CANCEL, cancelledAt);
    }

    private static EnrollmentHistory create(
            Enrollment enrollment,
            EnrollmentHistoryAction action,
            LocalDateTime createdAt
    ) {
        EnrollmentHistory history = new EnrollmentHistory();
        history.student = enrollment.getStudent();
        history.lecture = enrollment.getLecture();
        history.action = action;
        history.createdAt = createdAt;
        return history;
    }
}
