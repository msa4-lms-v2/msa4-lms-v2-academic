package com.msa4lmsv2academic.domain.attendance.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "학생 QR 출석 체크인 요청")
public record AttendanceCheckInRequestDTO(
        @NotNull(message = "출석 세션 ID는 필수입니다.")
        @Schema(description = "QR에 포함된 출석 세션 ID", example = "3")
        Long sessionId,

        @NotBlank(message = "QR 토큰은 필수입니다.")
        @Schema(description = "QR에 포함된 일회성 출석 토큰", example = "tHQsfOon75cNI1mPgglyMrUINqcgYEKJox5kuCMtfiI")
        String token
) {
}
