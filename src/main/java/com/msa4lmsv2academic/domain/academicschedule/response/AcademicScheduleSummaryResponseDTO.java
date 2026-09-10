package com.msa4lmsv2academic.domain.academicschedule.response;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleCategory;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Schema(description = "학사일정 목록 항목")
public record AcademicScheduleSummaryResponseDTO(
        @Schema(description = "학사일정 ID", example = "1") Long id,
        @Schema(description = "일정 제목", example = "2026학년도 2학기 수강신청") String title,
        @Schema(description = "일정 분류", example = "ENROLLMENT") AcademicScheduleCategory category,
        @Schema(description = "일정 분류명", example = "수강신청") String categoryLabel,
        @Schema(description = "서버가 시작일 기준으로 자동 지정한 학년도", example = "2026") short academicYear,
        @Schema(description = "서버가 시작일 기준으로 자동 지정한 학기", example = "SECOND") SemesterTerm term,
        @Schema(description = "일정 시작일", example = "2026-08-17", format = "date") LocalDate startDate,
        @Schema(description = "일정 종료일", example = "2026-08-21", format = "date", nullable = true) LocalDate endDate,
        @Schema(description = "공개 대상 역할", example = "STUDENT",
                allowableValues = {"ALL", "STUDENT", "PROFESSOR"}) AcademicScheduleTargetRole targetRole,
        @Schema(description = "활성 여부", example = "true") boolean isActive,
        @Schema(description = "등록 일시", example = "2026-08-14T14:00:00", format = "date-time") LocalDateTime createdAt
) {

    public static AcademicScheduleSummaryResponseDTO from(AcademicSchedule schedule) {
        return new AcademicScheduleSummaryResponseDTO(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getCategory(),
                schedule.getCategory().getLabel(),
                schedule.getAcademicYear(),
                schedule.getTerm(),
                schedule.getStartDate(),
                schedule.getEndDate(),
                schedule.getTargetRole(),
                schedule.isActive(),
                schedule.getCreatedAt()
        );
    }
}
