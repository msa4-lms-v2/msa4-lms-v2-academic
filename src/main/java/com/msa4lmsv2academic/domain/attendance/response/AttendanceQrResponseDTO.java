package com.msa4lmsv2academic.domain.attendance.response;

import java.time.LocalDateTime;

// QR 발급 결과
public record AttendanceQrResponseDTO(
        String qrUrl,
        LocalDateTime expiresAt,
        int refreshAfterSeconds
) {
}
