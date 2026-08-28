package com.msa4lmsv2academic.domain.dismissal.request;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "대기 후보 확정 또는 취소. 취소는 cancelReason 필수, 확정은 cancelReason 사용 불가")
public record DismissalStatusRequestDTO(
        @NotNull @PositiveOrZero @Schema(description = "조회한 최신 version", example = "0", requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @NotNull @Schema(description = "목표 상태", allowableValues = {"CONFIRMED", "CANCELLED"}, example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED) DismissalStatus status,
        @Size(max = 500) @Schema(description = "취소 시 필수 사유(1~500자), 확정 시 null", example = "확인 결과 제적 사유가 해소되었습니다.", nullable = true) String cancelReason
) {
    public DismissalStatusRequestDTO { cancelReason = cancelReason == null ? null : cancelReason.strip(); }
}
