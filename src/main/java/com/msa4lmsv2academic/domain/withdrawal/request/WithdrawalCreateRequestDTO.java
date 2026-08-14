package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "자퇴 신청 요청")
public record WithdrawalCreateRequestDTO(
        @NotBlank
        @Size(max = 500)
        String reason,

        @Schema(description = "학생 희망 자퇴 적용일. 최종 적용일은 관리자가 확정합니다.")
        LocalDate requestedEffectiveDate
) {
}
