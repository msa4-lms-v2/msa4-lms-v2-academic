package com.msa4lmsv2academic.domain.graduation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "졸업 학점요건 부분 수정 요청")
public record GraduationRequirementUpdateRequestDTO(
        @Schema(description = "변경할 학과 ID", example = "1")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "변경할 입학연도", example = "2024", minimum = "1900")
        @Min(value = 1900, message = "admissionYear는 1900 이상이어야 합니다.")
        Integer admissionYear,

        @Schema(description = "변경할 최소 전공학점", example = "60", minimum = "0", maximum = "300")
        @Min(value = 0, message = "requiredMajorCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredMajorCredits는 300 이하여야 합니다.")
        Integer requiredMajorCredits,

        @Schema(description = "변경할 최소 교양학점", example = "30", minimum = "0", maximum = "300")
        @Min(value = 0, message = "requiredGeneralCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredGeneralCredits는 300 이하여야 합니다.")
        Integer requiredGeneralCredits,

        @Schema(description = "변경할 최소 총학점", example = "130", minimum = "0", maximum = "300")
        @Min(value = 0, message = "requiredTotalCredits는 0 이상이어야 합니다.")
        @Max(value = 300, message = "requiredTotalCredits는 300 이하여야 합니다.")
        Integer requiredTotalCredits,

        @Schema(description = "변경 사유", example = "2027학년도 교육과정 개편 반영",
                minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {

    public boolean hasAnyUpdateField() {
        return departmentId != null || admissionYear != null || requiredMajorCredits != null
                || requiredGeneralCredits != null || requiredTotalCredits != null;
    }
}
