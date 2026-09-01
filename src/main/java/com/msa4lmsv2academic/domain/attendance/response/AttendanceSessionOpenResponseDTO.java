package com.msa4lmsv2academic.domain.attendance.response;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;

import java.time.LocalDateTime;

// 세션 생성 응답 DTO
public record AttendanceSessionOpenResponseDTO(
        Long sessionId,
        Long classId,
        String courseName, // 과목명
        String sectionNo, // 분반
        Integer period,
        AttendanceSessionStatus status,
        LocalDateTime openedAt,
        Long attendedCount, // 현재 출석 완료 학생 수
        Long totalEnrollmentCount, // 해당 강의 전체 수강생 수
        AttendanceQrResponseDTO qr // 현재 출석에 사용할 qr 정보
) {
}
