package com.msa4lmsv2academic.domain.counseling.entity;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.EnumSet;
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
        name = "counseling_appointments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_counseling_appointments_professor_time",
                        columnNames = {"professor_id", "appointment_at"}
                ),
                @UniqueConstraint(
                        name = "uk_counseling_appointments_student_time",
                        columnNames = {"student_id", "appointment_at"}
                )
        },
        indexes = {
                @Index(name = "idx_counseling_appointments_student_status", columnList = "student_id, status"),
                @Index(name = "idx_counseling_appointments_professor_status", columnList = "professor_id, status"),
                @Index(name = "idx_counseling_appointments_at", columnList = "appointment_at")
        }
)
public class CounselingAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(name = "appointment_at", nullable = false)
    private LocalDateTime appointmentAt;

    @Column(length = 255)
    private String topic;

    @Column(name = "professor_note", columnDefinition = "TEXT")
    private String professorNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CounselingAppointmentStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CounselingAppointment(
            Student student,
            Professor professor,
            LocalDateTime appointmentAt,
            String topic
    ) {
        this.student = student;
        this.professor = professor;
        this.appointmentAt = appointmentAt;
        this.topic = topic;
        this.status = CounselingAppointmentStatus.PENDING;
    }

    public static CounselingAppointment create(
            Student student,
            Professor professor,
            LocalDateTime appointmentAt,
            String topic
    ) {
        return new CounselingAppointment(student, professor, appointmentAt, topic);
    }

    public void changeStatus(CounselingAppointmentStatus nextStatus, String professorNote) {
        if (!allowedNextStatuses().contains(nextStatus)) {
            throw new IllegalStateException("허용되지 않은 상담 예약 상태 전이입니다.");
        }
        this.status = nextStatus;
        if (professorNote != null) {
            this.professorNote = professorNote;
        }
    }

    private EnumSet<CounselingAppointmentStatus> allowedNextStatuses() {
        return switch (status) {
            case PENDING -> EnumSet.of(
                    CounselingAppointmentStatus.CONFIRMED,
                    CounselingAppointmentStatus.CANCELLED,
                    CounselingAppointmentStatus.REJECTED
            );
            case CONFIRMED -> EnumSet.of(
                    CounselingAppointmentStatus.COMPLETED,
                    CounselingAppointmentStatus.CANCELLED
            );
            case COMPLETED -> EnumSet.of(CounselingAppointmentStatus.COMPLETED);
            case CANCELLED, REJECTED -> EnumSet.noneOf(CounselingAppointmentStatus.class);
        };
    }
}
