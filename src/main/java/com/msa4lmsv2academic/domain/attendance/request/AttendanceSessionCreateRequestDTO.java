package com.msa4lmsv2academic.domain.attendance.request;

import jakarta.validation.constraints.NotNull;

// 세션 생성 requestDTO
public record AttendanceSessionCreateRequestDTO(
        @NotNull(message = "강의 ID는 필수입니다.")
        Long classId
) {
}
