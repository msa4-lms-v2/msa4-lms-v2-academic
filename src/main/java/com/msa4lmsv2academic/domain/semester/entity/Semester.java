package com.msa4lmsv2academic.domain.semester.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "semesters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semesters_academic_year_term",
                columnNames = {"academic_year", "term"}
        )
)
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // Kafka SemesterCreated 이벤트의 sourceVersion. 학기는 생성 후 이 이벤트가 다루는 필드가 안 바뀌므로 항상 1로 고정.
    @Column(name = "snapshot_version", nullable = false)
    private long snapshotVersion;

    @Column(name = "academic_year", nullable = false)
    private short academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SemesterTerm term;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "enrollment_start_at", nullable = false)
    private LocalDateTime enrollmentStartAt;

    @Column(name = "enrollment_end_at", nullable = false)
    private LocalDateTime enrollmentEndAt;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    private Semester(short academicYear, SemesterTerm term, LocalDate startDate, LocalDate endDate,
                     LocalDateTime enrollmentStartAt, LocalDateTime enrollmentEndAt, boolean current) {
        this.academicYear = academicYear;
        this.term = term;
        this.startDate = startDate;
        this.endDate = endDate;
        this.enrollmentStartAt = enrollmentStartAt;
        this.enrollmentEndAt = enrollmentEndAt;
        this.current = current;
        this.snapshotVersion = 1L;
    }

    public static Semester create(short academicYear, SemesterTerm term, LocalDate startDate, LocalDate endDate,
                                  LocalDateTime enrollmentStartAt, LocalDateTime enrollmentEndAt,
                                  boolean current) {
        return new Semester(academicYear, term, startDate, endDate, enrollmentStartAt, enrollmentEndAt, current);
    }

    public void unsetCurrent() {
        this.current = false;
    }
}
