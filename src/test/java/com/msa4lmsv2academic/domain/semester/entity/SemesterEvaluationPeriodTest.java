package com.msa4lmsv2academic.domain.semester.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SemesterEvaluationPeriodTest {

    @Test
    void evaluationPeriodIncludesBothBoundaries() {
        LocalDateTime evaluationStart = LocalDateTime.of(2026, 6, 8, 9, 0);
        LocalDateTime evaluationEnd = LocalDateTime.of(2026, 6, 19, 18, 0);
        Semester semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 16, 9, 0),
                LocalDateTime.of(2026, 2, 20, 18, 0),
                evaluationStart,
                evaluationEnd,
                false
        );

        assertThat(semester.isEvaluationOpenAt(evaluationStart)).isTrue();
        assertThat(semester.isEvaluationOpenAt(evaluationEnd)).isTrue();
        assertThat(semester.isEvaluationOpenAt(evaluationStart.minusNanos(1))).isFalse();
        assertThat(semester.isEvaluationOpenAt(evaluationEnd.plusNanos(1))).isFalse();
    }

    @Test
    void legacySemesterWithoutEvaluationPeriodIsClosed() {
        Semester semester = Semester.create(
                (short) 2026,
                SemesterTerm.FIRST,
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 6, 19),
                LocalDateTime.of(2026, 2, 16, 9, 0),
                LocalDateTime.of(2026, 2, 20, 18, 0),
                false
        );

        assertThat(semester.isEvaluationOpenAt(LocalDateTime.of(2026, 6, 10, 12, 0))).isFalse();
    }
}
