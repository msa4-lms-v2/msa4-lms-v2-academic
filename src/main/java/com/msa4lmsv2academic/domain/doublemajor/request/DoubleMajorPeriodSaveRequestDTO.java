package com.msa4lmsv2academic.domain.doublemajor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record DoubleMajorPeriodSaveRequestDTO(
        @NotNull @Positive
        @Schema(description = "모집 회차의 기준 학기 ID. 학생의 적용 희망 학기가 아니며 등록 후 변경할 수 없습니다.",
                example = "23", requiredMode = Schema.RequiredMode.REQUIRED)
        Long semesterId,
        @NotNull
        @Schema(description = "접수 시작 일시(KST, 경계 포함)", example = "2026-12-01T09:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime startAt,
        @NotNull
        @Schema(description = "접수 종료 일시(KST, 시작보다 뒤, 경계 포함)", example = "2026-12-15T18:00:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime endAt,
        @NotNull
        @Schema(description = "활성 여부. false면 신규 신청만 차단하고 기존 신청은 보존합니다.", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean active,
        @NotBlank @Size(max = 255)
        @Schema(description = "기간 등록·변경 사유(1~255자), 감사 기록", minLength = 1, maxLength = 255,
                example = "2027학년도 1학기 복수전공 모집 기간 설정", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {
    public DoubleMajorPeriodSaveRequestDTO {
        reason = reason == null ? null : reason.strip();
    }

    @AssertTrue(message = "접수 시작 일시는 종료 일시보다 빨라야 합니다.")
    @Schema(hidden = true)
    public boolean isPeriodOrderValid() {
        return startAt == null || endAt == null || startAt.isBefore(endAt);
    }
}
