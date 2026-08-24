package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "선수과목 기준 전체 수정 요청")
public record PrerequisiteRetakeRuleUpdateRequestDTO(
        @Schema(description = "변경할 대상 교과목 ID", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "courseId는 필수입니다.")
        @Positive(message = "courseId는 양수여야 합니다.")
        Long courseId,

        @Schema(description = "변경할 선수 교과목 ID", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "prerequisiteCourseId는 필수입니다.")
        @Positive(message = "prerequisiteCourseId는 양수여야 합니다.")
        Long prerequisiteCourseId,

        @Schema(description = "수정 사유", example = "교육과정 개편", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {
}
