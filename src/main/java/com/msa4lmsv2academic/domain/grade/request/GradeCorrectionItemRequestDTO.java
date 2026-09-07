package com.msa4lmsv2academic.domain.grade.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GradeCorrectionItemRequestDTO(
        @NotNull @Positive Long enrollmentId,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal midtermScore,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal finalScore,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal assignmentScore,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal attendanceScore,
        @NotBlank @Size(max = 500) String reason
) {
    public GradeScoreRequestDTO toGradeScoreRequest() {
        return new GradeScoreRequestDTO(
                enrollmentId, midtermScore, finalScore, assignmentScore, attendanceScore
        );
    }

    public String normalizedReason() {
        return reason.strip();
    }

    public boolean hasCompleteScoresAndReason() {
        return enrollmentId != null
                && midtermScore != null
                && finalScore != null
                && assignmentScore != null
                && attendanceScore != null
                && reason != null
                && !reason.isBlank();
    }
}
