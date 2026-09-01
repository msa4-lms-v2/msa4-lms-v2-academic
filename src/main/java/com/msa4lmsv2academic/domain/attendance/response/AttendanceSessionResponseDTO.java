package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceSessionResponseDTO(
        Long sessionId,
        Long classId,
        String courseName,
        String sectionNo,
        LocalDate sessionDate,
        Integer period,
        AttendanceSessionStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {

    public static AttendanceSessionResponseDTO from(
            AttendanceSession session
    ) {
        return new AttendanceSessionResponseDTO(
                session.getId(),
                session.getLecture().getId(),
                session.getLecture().getCourse().getName(),
                session.getLecture().getSectionNo(),
                session.getSessionDate(),
                session.getPeriod(),
                session.getStatus(),
                session.getOpenedAt(),
                session.getClosedAt()
        );
    }
}
