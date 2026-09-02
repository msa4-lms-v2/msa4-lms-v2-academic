package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "배정 지도교수의 1차 검토. PENDING 신청만 처리 가능합니다.")
public record AdvisorWithdrawalReviewRequestDTO(
        @Schema(description = "true 승인, false 반려", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @NotNull Boolean approved,
        @Schema(description = "반려 시 필수 사유. 승인 시 사용하지 않음", minLength = 1, maxLength = 500,
                nullable = true, example = "상담 후 다시 신청해 주세요.")
        @Size(max = 500) String rejectReason
) {
    public AdvisorWithdrawalReviewRequestDTO {
        rejectReason = rejectReason == null ? null : rejectReason.strip();
    }
}
