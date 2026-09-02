package com.msa4lmsv2academic.domain.doublemajor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoubleMajorCancelRequestDTO(
        @NotBlank @Size(max = 500)
        @Schema(description = "학생 본인의 신청 취소 사유(1~500자)", minLength = 1, maxLength = 500,
                example = "진로 계획을 다시 검토하기로 했습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
    public DoubleMajorCancelRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
