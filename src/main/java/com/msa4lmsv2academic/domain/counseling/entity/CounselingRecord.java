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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "counseling_records")
public class CounselingRecord {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "counseling_method", nullable = false, length = 20)
    private CounselingMethod counselingMethod;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "student_content", columnDefinition = "TEXT")
    private String studentContent;

    @Column(name = "professor_response", columnDefinition = "TEXT")
    private String professorResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CounselingStatus status;

    @Column(name = "counseled_at")
    private LocalDateTime counseledAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CounselingRecord(
            Student student,
            Professor professor,
            CounselingMethod counselingMethod,
            String title,
            String studentContent,
            String professorResponse,
            CounselingStatus status,
            LocalDateTime counseledAt,
            LocalDateTime respondedAt
    ) {
        this.student = student;
        this.professor = professor;
        this.counselingMethod = counselingMethod;
        this.title = title;
        this.studentContent = studentContent;
        this.professorResponse = professorResponse;
        this.status = status;
        this.counseledAt = counseledAt;
        this.respondedAt = respondedAt;
    }

    public static CounselingRecord requestOnline(
            Student student,
            Professor professor,
            String title,
            String studentContent
    ) {
        return new CounselingRecord(
                student,
                professor,
                CounselingMethod.ONLINE,
                title,
                studentContent,
                null,
                CounselingStatus.PENDING,
                null,
                null
        );
    }

    public static CounselingRecord recordInPerson(
            Student student,
            Professor professor,
            String title,
            String professorResponse,
            LocalDateTime counseledAt
    ) {
        return new CounselingRecord(
                student,
                professor,
                CounselingMethod.IN_PERSON,
                title,
                null,
                professorResponse,
                CounselingStatus.COMPLETED,
                counseledAt,
                null
        );
    }

    public void answer(String response, LocalDateTime answeredAt) {
        if (counselingMethod != CounselingMethod.ONLINE) {
            throw new IllegalStateException("온라인 상담에만 답변할 수 있습니다.");
        }
        if (status == CounselingStatus.CANCELLED) {
            throw new IllegalStateException("취소된 상담에는 답변할 수 없습니다.");
        }
        this.professorResponse = response;
        this.respondedAt = answeredAt;
        this.counseledAt = answeredAt;
        this.status = CounselingStatus.COMPLETED;
    }
}
