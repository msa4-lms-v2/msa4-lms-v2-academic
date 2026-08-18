package com.msa4lmsv2academic.domain.lecture.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lecture_opening_request_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_opening_request_schedules_slot",
                columnNames = {"request_id", "day_of_week", "start_period", "end_period"}
        )
)
public class LectureOpeningRequestSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private LectureOpeningRequest openingRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private LectureDayOfWeek dayOfWeek;

    @Column(name = "start_period", nullable = false)
    private byte startPeriod;

    @Column(name = "end_period", nullable = false)
    private byte endPeriod;

    private LectureOpeningRequestSchedule(
            LectureOpeningRequest openingRequest,
            LectureDayOfWeek dayOfWeek,
            byte startPeriod,
            byte endPeriod
    ) {
        this.openingRequest = openingRequest;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
    }

    public static LectureOpeningRequestSchedule create(
            LectureOpeningRequest openingRequest,
            LectureDayOfWeek dayOfWeek,
            byte startPeriod,
            byte endPeriod
    ) {
        return new LectureOpeningRequestSchedule(openingRequest, dayOfWeek, startPeriod, endPeriod);
    }
}
