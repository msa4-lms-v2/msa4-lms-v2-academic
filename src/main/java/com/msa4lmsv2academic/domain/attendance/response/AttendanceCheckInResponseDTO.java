package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceCheckInResponseDTO(
        Long attendanceId,
        Long sessionId,
        Long classId,
        String courseName,
        AttendanceStatus status,
        LocalDateTime checkinTime
) {
}
