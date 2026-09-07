package com.msa4lmsv2academic.domain.attendance.request;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "출결 기록 수정 요청")
public record AttendanceRecordUpdateRequestDTO(
        @Schema(description = "변경할 출결 상태", example = "LATE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "출결 상태는 필수입니다.")
        AttendanceStatus status,

        @Schema(description = "출결 비고. 빈 문자열을 보내면 비고를 삭제합니다.", example = "교통 지연 확인")
        @Size(max = 255, message = "비고는 255자 이하여야 합니다.")
        String remarks,

        @Schema(description = "수정 사유", example = "출석 확인 후 지각으로 정정", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "수정 사유는 필수입니다.")
        @Size(max = 255, message = "수정 사유는 255자 이하여야 합니다.")
        String reason
) {
}
