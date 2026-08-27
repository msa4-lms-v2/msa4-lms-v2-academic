package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "본인 자퇴 신청 취소 전용 요청. 다른 상태로 변경할 수 없습니다.")
public record WithdrawalCancelRequestDTO(
        @Schema(description = "취소 사유. 앞뒤 공백 제거 후 1~255자, 내부 띄어쓰기 허용",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 255,
                example = "학업을 계속하기로 결정했습니다.")
        @NotBlank @Size(max = 255) String cancelReason
) {
    public WithdrawalCancelRequestDTO {
        cancelReason = cancelReason == null ? null : cancelReason.strip();
    }
}

