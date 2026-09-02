package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity @Getter @EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED) @EntityListeners(AuditingEntityListener.class)
@Table(name = "enrollments", indexes = {@Index(name = "idx_enrollments_student_id", columnList = "student_id"), @Index(name = "idx_enrollments_lecture_id", columnList = "lecture_id")})
public class Enrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lecture_id", nullable = false) private Lecture lecture;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EnrollmentStatus status;
    @Column(name = "enrolled_at", nullable = false) private LocalDateTime enrolledAt;
    @Column(name = "midterm_score", precision = 5, scale = 2) private BigDecimal midtermScore;
    @Column(name = "final_score", precision = 5, scale = 2) private BigDecimal finalScore;
    @Column(name = "assignment_score", precision = 5, scale = 2) private BigDecimal assignmentScore;
    @Column(name = "attendance_score", precision = 5, scale = 2) private BigDecimal attendanceScore;
    @Column(name = "total_score", precision = 5, scale = 2) private BigDecimal totalScore;
    @Column(name = "letter_grade", length = 5) private String letterGrade;
    @Enumerated(EnumType.STRING) @Column(name = "grade_status", nullable = false, length = 20) private GradeStatus gradeStatus;

    private Enrollment(Student student, Lecture lecture, EnrollmentStatus status, LocalDateTime enrolledAt,
                       GradeStatus gradeStatus) {
        this.student = student;
        this.lecture = lecture;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.gradeStatus = gradeStatus;
    }

    public static Enrollment create(Student student, Lecture lecture, LocalDateTime enrolledAt) {
        return new Enrollment(student, lecture, EnrollmentStatus.ACTIVE, enrolledAt, GradeStatus.DRAFT);
    }

    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
    }
}
