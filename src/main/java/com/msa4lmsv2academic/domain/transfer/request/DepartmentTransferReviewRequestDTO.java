package com.msa4lmsv2academic.domain.transfer.request;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepartmentTransferReviewRequestDTO(
        @NotNull
        @Schema(description = "심사 결과. APPROVED 또는 REJECTED", example = "APPROVED",
                requiredMode = Schema.RequiredMode.REQUIRED)
        AcademicChangeRequestStatus status,
        @Size(max = 500)
        @Schema(description = "반려 사유. REJECTED일 때 필수(1~500자)", maxLength = 500,
                example = "필수 제출 서류의 내용이 확인되지 않습니다.")
        String reason
) {
    public DepartmentTransferReviewRequestDTO {
        reason = reason == null ? null : reason.strip();
    }

    @AssertTrue(message = "APPROVED 또는 사유가 있는 REJECTED만 허용됩니다.")
    @Schema(hidden = true)
    public boolean isValidDecision() {
        return status == AcademicChangeRequestStatus.APPROVED
                || status == AcademicChangeRequestStatus.REJECTED && reason != null && !reason.isBlank();
    }
}
