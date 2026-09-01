package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 출석 세션 내역
public record AttendanceSessionListResponseDTO(
        Long sessionId,
        Long classId,
        String courseName,
        String sectionNo,
        LocalDate sessionDate,
        Integer period,
        AttendanceSessionStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        long attendedCount,
        long totalEnrollmentCount
) {

    public static AttendanceSessionListResponseDTO of(
            AttendanceSession session,
            long attendedCount,
            long totalEnrollmentCount
    ) {
        return new AttendanceSessionListResponseDTO(
                session.getId(),
                session.getLecture().getId(),
                session.getLecture().getCourse().getName(),
                session.getLecture().getSectionNo(),
                session.getSessionDate(),
                session.getPeriod(),
                session.getStatus(),
                session.getOpenedAt(),
                session.getClosedAt(),
                attendedCount,
                totalEnrollmentCount
        );
    }
}
