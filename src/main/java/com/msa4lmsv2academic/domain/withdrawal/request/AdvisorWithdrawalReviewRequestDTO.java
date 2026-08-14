package com.msa4lmsv2academic.domain.withdrawal.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "지도교수 자퇴 검토 요청")
public record AdvisorWithdrawalReviewRequestDTO(
        @NotNull
        Boolean approved,

        @Size(max = 500)
        String rejectReason
) {
}
