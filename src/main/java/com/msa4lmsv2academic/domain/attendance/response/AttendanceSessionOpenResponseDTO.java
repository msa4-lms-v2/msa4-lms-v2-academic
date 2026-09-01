package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "출석 세션 생성 또는 재개 결과")
public record AttendanceSessionOpenResponseDTO(
        @Schema(description = "출석 세션 ID", example = "3")
        Long sessionId,
        @Schema(description = "강의 ID", example = "8")
        Long classId,
        @Schema(description = "과목명", example = "소프트웨어공학")
        String courseName,
        @Schema(description = "분반", example = "01")
        String sectionNo,
        @Schema(description = "대표 시작 교시", example = "1")
        Integer period,
        @Schema(description = "세션 상태", example = "OPEN")
        AttendanceSessionStatus status,
        @Schema(description = "세션 시작 또는 재개 시각", example = "2026-09-01T11:00:00")
        LocalDateTime openedAt,
        @Schema(description = "현재 출석 완료 인원", example = "0")
        Long attendedCount,
        @Schema(description = "전체 활성 수강생 수", example = "40")
        Long totalEnrollmentCount,
        @Schema(description = "현재 사용할 출석 QR 정보")
        AttendanceQrResponseDTO qr
) {
}
