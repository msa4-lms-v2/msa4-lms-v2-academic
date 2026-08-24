package com.msa4lmsv2academic.domain.graduation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "학과·입학연도별 졸업 학점요건 등록 요청")
public record GraduationRequirementCreateRequestDTO(
        @Schema(description = "학과 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "departmentId는 필수입니다.")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "입학연도", example = "2024", minimum = "1900",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "admissionYear는 필수입니다.")
        @Min(value = 1900, message = "admissionYear는 1900 이상이어야 합니다.")
        Integer admissionYear,

        @Schema(description = "최소 전공학점", example = "60", minimum = "0", maximum = "300",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "requiredMajorCredits는 필수입니다.")
        @Min(value = 0, message = "requiredMajorCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredMajorCredits는 300 이하여야 합니다.")
        Integer requiredMajorCredits,

        @Schema(description = "최소 교양학점", example = "30", minimum = "0", maximum = "300",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "requiredGeneralCredits는 필수입니다.")
        @Min(value = 0, message = "requiredGeneralCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredGeneralCredits는 300 이하여야 합니다.")
        Integer requiredGeneralCredits,

        @Schema(description = "최소 총학점", example = "130", minimum = "0", maximum = "300",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "requiredTotalCredits는 필수입니다.")
        @Min(value = 0, message = "requiredTotalCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredTotalCredits는 300 이하여야 합니다.")
        Integer requiredTotalCredits
) {
}
