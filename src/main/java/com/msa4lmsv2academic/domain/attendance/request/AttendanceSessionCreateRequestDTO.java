package com.msa4lmsv2academic.domain.attendance.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "출석 세션 생성 요청")
public record AttendanceSessionCreateRequestDTO(
        @NotNull(message = "강의 ID는 필수입니다.")
        @Schema(description = "출석 세션을 생성할 현재 학기 담당 강의 ID", example = "8")
        Long classId
) {
}
