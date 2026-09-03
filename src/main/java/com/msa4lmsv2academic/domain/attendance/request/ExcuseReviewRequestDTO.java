package com.msa4lmsv2academic.domain.attendance.request;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "담당 교수 공결 승인·반려 요청")
public record ExcuseReviewRequestDTO(
        @Schema(description = "처리 상태. APPROVED 또는 REJECTED만 허용됩니다.", example = "APPROVED")
        @NotNull(message = "처리 상태는 필수입니다.")
        ExcuseRequestStatus status,

        @Schema(description = "반려 사유. REJECTED일 때 필수입니다.", example = "증빙 자료를 확인할 수 없습니다.")
        @Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.")
        String rejectReason
) {

    @Schema(hidden = true)
    @AssertTrue(message = "처리 상태는 APPROVED 또는 REJECTED만 사용할 수 있습니다.")
    public boolean isReviewStatusValid() {
        return status == null
                || status == ExcuseRequestStatus.APPROVED
                || status == ExcuseRequestStatus.REJECTED;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "반려 시 반려 사유는 필수입니다.")
    public boolean isRejectReasonValid() {
        return status == null
                || status != ExcuseRequestStatus.REJECTED
                || (rejectReason != null && !rejectReason.isBlank());
    }
}
