package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;

import java.time.LocalDateTime;

public record AttendanceSessionCloseResponseDTO(
        Long sessionId,
        AttendanceSessionStatus status,
        LocalDateTime openedAt,
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
