package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "관리자 자퇴 최종 검토 요청")
public record FinalWithdrawalReviewRequestDTO(
        @NotNull
        Boolean approved,

        @Schema(description = "승인 시 확정 자퇴 적용일")
        LocalDate effectiveDate,

        @Size(max = 500)
        String rejectReason
) {
}
