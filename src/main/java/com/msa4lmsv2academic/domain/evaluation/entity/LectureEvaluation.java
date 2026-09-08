package com.msa4lmsv2academic.domain.evaluation.entity;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "lecture_evaluations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_evaluations_enrollment",
                columnNames = "enrollment_id"
        )
)
public class LectureEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Integer> ratings;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LectureEvaluation(
            Enrollment enrollment,
            Map<String, Integer> ratings,
            String comment,
            LocalDateTime submittedAt
    ) {
        this.enrollment = enrollment;
        this.ratings = new LinkedHashMap<>(ratings);
        this.comment = normalizeComment(comment);
        this.submittedAt = submittedAt;
    }

    public static LectureEvaluation create(
            Enrollment enrollment,
            Map<String, Integer> ratings,
            String comment,
            LocalDateTime submittedAt
    ) {
        if (enrollment == null || ratings == null || ratings.isEmpty() || submittedAt == null) {
            throw new IllegalArgumentException("강의평가 생성에 필요한 값이 누락되었습니다.");
        }
        return new LectureEvaluation(enrollment, ratings, comment, submittedAt);
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
