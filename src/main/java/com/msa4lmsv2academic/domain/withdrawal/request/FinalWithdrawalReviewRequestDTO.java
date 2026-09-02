package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "ADMIN 최종 검토. ADVISOR_APPROVED 신청만 처리 가능합니다.")
public record FinalWithdrawalReviewRequestDTO(
        @Schema(description = "true 승인, false 반려", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @NotNull Boolean approved,
        @Schema(description = "승인 시 필수. 승인 당일(KST)만 허용하며 학생 희망일보다 이를 수 없습니다. 반려 시 사용하지 않음",
                format = "date", nullable = true, example = "2026-09-01")
        LocalDate effectiveDate,
        @Schema(description = "반려 시 필수 사유. 승인 시 사용하지 않음", minLength = 1, maxLength = 500,
                nullable = true, example = "추가 확인이 필요합니다.")
        @Size(max = 500) String rejectReason
) {
    public FinalWithdrawalReviewRequestDTO {
        rejectReason = rejectReason == null ? null : rejectReason.strip();
    }
}
