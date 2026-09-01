package com.msa4lmsv2academic.domain.attendance.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceCheckInRequestDTO(
        @NotNull Long sessionId,
        @NotBlank String token
) {
}
