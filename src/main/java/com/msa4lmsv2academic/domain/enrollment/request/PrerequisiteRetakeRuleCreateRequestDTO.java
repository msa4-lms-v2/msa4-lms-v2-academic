package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "선수과목 기준 등록 요청")
public record PrerequisiteRetakeRuleCreateRequestDTO(
        @Schema(description = "대상 교과목 ID", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "courseId는 필수입니다.")
        @Positive(message = "courseId는 양수여야 합니다.")
        Long courseId,

        @Schema(description = "선수 교과목 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "prerequisiteCourseId는 필수입니다.")
        @Positive(message = "prerequisiteCourseId는 양수여야 합니다.")
        Long prerequisiteCourseId,

        @Schema(description = "등록·재활성화 사유", example = "2027학년도 교육과정 반영", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {
}
