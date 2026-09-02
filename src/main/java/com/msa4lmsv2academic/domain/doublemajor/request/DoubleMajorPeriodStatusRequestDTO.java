package com.msa4lmsv2academic.domain.doublemajor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record DoubleMajorPeriodStatusRequestDTO(
        @NotNull @Schema(description = "활성 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean active,
        @NotBlank @Size(max = 255)
        @Schema(description = "활성 상태 변경 사유(1~255자), 감사 기록", minLength = 1, maxLength = 255,
                example = "모집 조기 종료", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
    public DoubleMajorPeriodStatusRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
