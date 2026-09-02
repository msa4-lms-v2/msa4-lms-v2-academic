package com.msa4lmsv2academic.domain.doublemajor.response;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DoubleMajorPeriodResponseDTO(
        @Schema(description = "모집 회차 ID", example = "2") Long id,
        @Schema(description = "모집 회차 기준 학기 ID. 학생의 적용 희망 학기가 아닙니다.", example = "23") Long semesterId,
        @Schema(description = "모집 회차 기준 학년도", example = "2027") short academicYear,
        @Schema(description = "모집 회차 기준 학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "접수 시작(KST, 경계 포함)", example = "2026-12-01T09:00:00") LocalDateTime startAt,
        @Schema(description = "접수 종료(KST, 경계 포함)", example = "2026-12-15T18:00:00") LocalDateTime endAt,
        @Schema(description = "활성 여부", example = "true") boolean active,
        @Schema(description = "현재 접수 가능 여부", example = "true") boolean open,
        @Schema(description = "등록 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime createdAt,
        @Schema(description = "수정 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime updatedAt
) {
    public static DoubleMajorPeriodResponseDTO from(AcademicChangeRequestPeriod period, LocalDateTime now) {
        return new DoubleMajorPeriodResponseDTO(
                period.getId(),
                period.getSemester().getId(),
                period.getSemester().getAcademicYear(),
                period.getSemester().getTerm(),
                period.getStartAt(),
                period.getEndAt(),
                period.isActive(),
                period.accepts(now),
                period.getCreatedAt(),
                period.getUpdatedAt());
    }
}
