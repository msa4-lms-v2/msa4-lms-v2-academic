package com.msa4lmsv2academic.domain.attendance.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "출석 QR 발급 결과")
public record AttendanceQrResponseDTO(
        @Schema(description = "QR 이미지로 변환할 학생 체크인 URL", example = "http://localhost:3000/attendance/check-in?sessionId=3&token=abc123")
        String qrUrl,
        @Schema(description = "QR 토큰 만료 시각", example = "2026-09-01T11:47:30")
        LocalDateTime expiresAt,
        @Schema(description = "프론트엔드가 다음 QR을 요청할 권장 시간(초)", example = "10")
        int refreshAfterSeconds
) {
}
