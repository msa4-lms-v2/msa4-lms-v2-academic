package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "최대 신청학점 규칙 전체 수정 요청")
public record EnrollmentCreditLimitRuleUpdateRequestDTO(
        @Schema(description = "변경할 최대 신청학점", example = "21", minimum = "1", maximum = "30",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "maxCredits는 필수입니다.")
        @Min(value = 1, message = "maxCredits는 1 이상이어야 합니다.")
        @Max(value = 30, message = "maxCredits는 30 이하여야 합니다.")
        Integer maxCredits,

        @Schema(description = "수정 사유", example = "학사 운영위원회 확정안 반영", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {
}
