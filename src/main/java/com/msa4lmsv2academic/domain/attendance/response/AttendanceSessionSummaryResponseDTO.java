package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "출석 세션 참여 현황")
public record AttendanceSessionSummaryResponseDTO(
        @Schema(description = "출석 세션 ID", example = "3")
        Long sessionId,
        @Schema(description = "세션 상태", example = "OPEN")
        AttendanceSessionStatus status,
        @Schema(description = "현재 출석 완료 인원", example = "27")
        long attendedCount,
        @Schema(description = "전체 활성 수강생 수", example = "40")
        long totalEnrollmentCount
) {
}
