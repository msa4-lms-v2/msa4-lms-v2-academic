package com.msa4lmsv2academic.domain.enrollment.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "학생 본인 시간표 조회 조건")
public record StudentTimetableSearchRequestDTO(
        @Schema(description = "조회 학년도", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "academicYear는 필수입니다.")
        @Min(value = 1900, message = "academicYear는 1900 이상이어야 합니다.")
        @Max(value = 9999, message = "academicYear는 9999 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "조회 학기", example = "FIRST", allowableValues = {"FIRST", "SECOND"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "term은 필수입니다.")
        SemesterTerm term
) {
}
