package com.msa4lmsv2academic.domain.lecture.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lecture_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_schedules_slot",
                columnNames = {"lecture_id", "day_of_week", "start_period"}
        ),
        indexes = @Index(name = "idx_lecture_schedules_lecture_id", columnList = "lecture_id")
)
public class LectureSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private LectureDayOfWeek dayOfWeek;

    @Column(name = "start_period", nullable = false)
    private byte startPeriod;

    @Column(name = "end_period", nullable = false)
    private byte endPeriod;

    private LectureSchedule(Lecture lecture, LectureDayOfWeek dayOfWeek, byte startPeriod, byte endPeriod) {
        this.lecture = lecture;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
    }

    public static LectureSchedule create(
            Lecture lecture,
            LectureDayOfWeek dayOfWeek,
            byte startPeriod,
            byte endPeriod
    ) {
        return new LectureSchedule(lecture, dayOfWeek, startPeriod, endPeriod);
    }
}
