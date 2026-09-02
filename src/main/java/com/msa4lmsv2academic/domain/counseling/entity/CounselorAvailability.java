package com.msa4lmsv2academic.domain.counseling.entity;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "counselor_availabilities",
        indexes = {
                @Index(
                        name = "idx_counselor_availabilities_professor_day",
                        columnList = "professor_id, day_of_week"
                ),
                @Index(
                        name = "idx_counselor_availabilities_validity",
                        columnList = "valid_from, valid_to"
                )
        }
)
public class CounselorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private CounselorAvailability(
            Professor professor,
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        this.professor = professor;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static CounselorAvailability create(
            Professor professor,
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        return new CounselorAvailability(professor, dayOfWeek, startTime, endTime, validFrom, validTo);
    }
}
