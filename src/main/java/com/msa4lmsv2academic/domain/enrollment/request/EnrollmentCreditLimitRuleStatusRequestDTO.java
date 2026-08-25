package com.msa4lmsv2academic.domain.enrollment.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "최대 신청학점 규칙 활성 상태 변경 요청")
public record EnrollmentCreditLimitRuleStatusRequestDTO(
        @Schema(description = "변경할 활성 여부", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "active는 필수입니다.")
        Boolean active,

        @Schema(description = "상태 변경 사유", example = "학기 운영 계획 변경", maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {
}
