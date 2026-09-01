package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;

// 현재 참여 인원 관련 response
public record AttendanceSessionSummaryResponseDTO(
        Long sessionId,
        AttendanceSessionStatus status,
        long attendedCount,
        long totalEnrollmentCount
) {
}
