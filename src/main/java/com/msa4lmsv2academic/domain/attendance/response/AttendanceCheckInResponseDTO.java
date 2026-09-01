package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "학생 QR 출석 처리 결과")
public record AttendanceCheckInResponseDTO(
        @Schema(description = "출석 기록 ID", example = "1")
        Long attendanceId,
        @Schema(description = "출석 세션 ID", example = "3")
        Long sessionId,
        @Schema(description = "강의 ID", example = "8")
        Long classId,
        @Schema(description = "과목명", example = "소프트웨어공학")
        String courseName,
        @Schema(description = "출석 상태", example = "PRESENT")
        AttendanceStatus status,
        @Schema(description = "체크인 시각", example = "2026-09-01T11:47:11")
        LocalDateTime checkinTime
) {
}
