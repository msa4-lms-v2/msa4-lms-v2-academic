package com.msa4lmsv2academic.domain.transfer.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record FinalDepartmentTransferReviewRequestDTO(
        @Schema(description = "최종 승인 여부. false이면 rejectReason 필수", example = "true") Boolean approved,
        @Schema(description = "관리자 최종 반려 사유(1~500자)", example = "학적 변경 요건을 충족하지 못했습니다.") String rejectReason
) {
    public boolean isValidDecision() {
        return approved != null && (approved || rejectReason != null && !rejectReason.isBlank()
                && rejectReason.length() <= 500);
    }
}
