package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveRequestStatusChangeRequestDTO(
        @NotNull @Schema(description = "학생: CANCELLED, 관리자: APPROVED 또는 REJECTED", allowableValues = {"APPROVED", "REJECTED", "CANCELLED"}, example = "CANCELLED", requiredMode = Schema.RequiredMode.REQUIRED)
        LeaveRequestStatus status,
        @Size(max = 500) @Schema(description = "취소·반려 시 필수(1~500자). 승인 시 생략", maxLength = 500, example = "신청을 철회합니다.")
        String reason
) {
    public LeaveRequestStatusChangeRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
