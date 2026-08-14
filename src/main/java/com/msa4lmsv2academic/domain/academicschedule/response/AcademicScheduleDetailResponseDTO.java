package com.msa4lmsv2academic.domain.academicschedule.response;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "학사일정 상세 응답")
public record AcademicScheduleDetailResponseDTO(
        @Schema(description = "학사일정 ID", example = "1") Long id,
        @Schema(description = "일정 제목", example = "2026학년도 2학기 수강신청") String title,
        @Schema(description = "일정 상세 내용", example = "수강신청 기간과 유의사항을 확인해 주세요.", nullable = true) String content,
        @Schema(description = "일정 시작일", example = "2026-08-17", format = "date") LocalDate startDate,
        @Schema(description = "일정 종료일", example = "2026-08-21", format = "date", nullable = true) LocalDate endDate,
        @Schema(description = "공개 대상 역할", example = "STUDENT",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"}) AcademicScheduleTargetRole targetRole,
        @Schema(description = "활성 여부", example = "true") boolean isActive,
        @Schema(description = "등록 관리자의 Academic 사용자 ID", example = "3") Long authorId,
        @Schema(description = "등록 일시", example = "2026-08-14T14:00:00", format = "date-time") LocalDateTime createdAt
) {

    public static AcademicScheduleDetailResponseDTO from(AcademicSchedule schedule) {
        return new AcademicScheduleDetailResponseDTO(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getTargetRole(),
                schedule.isActive(),
                schedule.getAuthor().getId(),
                schedule.getCreatedAt()
        );
    }
}
