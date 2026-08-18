package com.msa4lmsv2academic.domain.enrollment.request;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "학생 본인 강의 조회 조건")
public record StudentEnrollmentSearchRequestDTO(
        @Schema(description = "학년도. 생략하면 전체 학년도를 조회합니다.", example = "2026")
        @Min(value = 1900, message = "academicYear는 1900 이상이어야 합니다.")
        @Max(value = 9999, message = "academicYear는 9999 이하여야 합니다.")
        Short academicYear,

        @Schema(description = "학기. 생략하면 전체 학기를 조회합니다.", example = "FIRST",
                allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term
) {
}
