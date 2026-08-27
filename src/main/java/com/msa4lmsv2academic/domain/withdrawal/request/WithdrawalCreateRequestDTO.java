package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "학생 본인 자퇴 신청. 연중 접수하며 실제 적용일은 관리자가 승인하는 당일입니다.")
public record WithdrawalCreateRequestDTO(
        @Schema(description = "신청 사유. 공백만 입력 불가", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1, maxLength = 500, example = "개인 사정")
        @NotBlank @Size(max = 500) String reason,
        @Schema(description = "선택 희망일. 입력 시 신청 당일(KST) 또는 미래 날짜. 희망일 전 최종 승인은 차단하며 자동 승인하지 않습니다.",
                format = "date", nullable = true, example = "2026-09-01")
        LocalDate requestedEffectiveDate
) {
    public WithdrawalCreateRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
