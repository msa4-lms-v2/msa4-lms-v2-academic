package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "출석 세션 종료 결과")
public record AttendanceSessionCloseResponseDTO(
        @Schema(description = "출석 세션 ID", example = "3")
        Long sessionId,
        @Schema(description = "세션 상태", example = "CLOSED")
        AttendanceSessionStatus status,
        @Schema(description = "세션 시작 시각", example = "2026-09-01T11:00:00")
        LocalDateTime openedAt,
        @Schema(description = "세션 종료 시각", example = "2026-09-01T11:50:00")
        LocalDateTime closedAt
) {
    public static AttendanceSessionCloseResponseDTO from(
            AttendanceSession session
    ) {
        return new AttendanceSessionCloseResponseDTO(
                session.getId(),
                session.getStatus(),
                session.getOpenedAt(),
                session.getClosedAt()
        );
    }
}
