package com.msa4lmsv2academic.domain.transfer.response;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record DepartmentTransferPeriodResponseDTO(
        @Schema(description = "기간 ID", example = "1") Long id,
        @Schema(description = "적용 학기 ID", example = "23") Long semesterId,
        @Schema(description = "적용 학년도", example = "2027") short academicYear,
        @Schema(description = "적용 학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "접수 시작(KST, 경계 포함)", example = "2026-12-01T09:00:00") LocalDateTime startAt,
        @Schema(description = "접수 종료(KST, 경계 포함)", example = "2026-12-15T18:00:00") LocalDateTime endAt,
        @Schema(description = "활성 여부", example = "true") boolean active,
        @Schema(description = "현재 접수 가능 여부", example = "true") boolean open,
        @Schema(description = "등록 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime createdAt,
        @Schema(description = "수정 시각(KST)", example = "2026-11-01T09:00:00") LocalDateTime updatedAt
) {
    public static DepartmentTransferPeriodResponseDTO from(AcademicChangeRequestPeriod period, LocalDateTime now) {
        return new DepartmentTransferPeriodResponseDTO(period.getId(), period.getSemester().getId(),
                period.getSemester().getAcademicYear(), period.getSemester().getTerm(), period.getStartAt(),
                period.getEndAt(), period.isActive(), period.accepts(now), period.getCreatedAt(), period.getUpdatedAt());
    }
}
