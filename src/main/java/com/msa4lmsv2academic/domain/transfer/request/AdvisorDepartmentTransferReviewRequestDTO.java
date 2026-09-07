package com.msa4lmsv2academic.domain.transfer.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdvisorDepartmentTransferReviewRequestDTO(
        @Schema(description = "승인 여부. false이면 rejectReason 필수", example = "true") Boolean approved,
        @Schema(description = "지도교수 반려 사유(1~500자)", example = "학업 계획 보완이 필요합니다.") String rejectReason
) {
    public boolean isValidDecision() {
        return approved != null && (approved || rejectReason != null && !rejectReason.isBlank()
                && rejectReason.length() <= 500);
    }
}
