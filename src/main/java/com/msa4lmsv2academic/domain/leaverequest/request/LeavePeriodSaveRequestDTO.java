package com.msa4lmsv2academic.domain.leaverequest.request;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record LeavePeriodSaveRequestDTO(
        @NotNull @Positive @Schema(description = "적용 학기 ID. 등록 후 변경 불가", example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
        Long semesterId,
        @NotNull @Schema(description = "신청 유형. 등록 후 변경 불가", example = "GENERAL_LEAVE", requiredMode = Schema.RequiredMode.REQUIRED)
        LeaveRequestType requestType,
        @NotNull @Schema(description = "접수 시작 일시(KST), 경계 포함", example = "2026-12-01T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime startAt,
        @NotNull @Schema(description = "접수 종료 일시(KST), 시작보다 뒤, 경계 포함", example = "2026-12-31T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime endAt,
        @NotNull @Schema(description = "승인 허용 시작 일시(KST), 경계 포함", example = "2027-03-02T09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime approvalStartAt,
        @NotNull @Schema(description = "승인 허용 종료 일시(KST), 시작보다 뒤, 경계 포함", example = "2027-03-10T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime approvalEndAt,
        @NotNull @Schema(description = "설정 활성 여부. 비활성이면 신청·승인 차단, 기존 신청은 보존", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean active,
        @NotBlank @Size(max = 255) @Schema(description = "설정 등록·변경 사유(1~255자), 감사 기록", minLength = 1, maxLength = 255, example = "학기별 접수 일정 설정", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
    public LeavePeriodSaveRequestDTO {
        reason = reason == null ? null : reason.strip();
    }

    @AssertTrue(message = "접수·승인 시작 일시는 각각 종료 일시보다 빨라야 합니다.")
    @Schema(hidden = true)
    public boolean isPeriodOrderValid() {
        return (startAt == null || endAt == null || startAt.isBefore(endAt))
                && (approvalStartAt == null || approvalEndAt == null || approvalStartAt.isBefore(approvalEndAt));
    }
}
