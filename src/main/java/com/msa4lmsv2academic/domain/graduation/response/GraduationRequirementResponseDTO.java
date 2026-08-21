package com.msa4lmsv2academic.domain.graduation.response;

import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "학과·입학연도별 졸업 학점요건")
public record GraduationRequirementResponseDTO(
        @Schema(description = "졸업요건 ID", example = "10") Long id,
        @Schema(description = "학과 ID", example = "1") Long departmentId,
        @Schema(description = "학과 코드", example = "001") String departmentCode,
        @Schema(description = "학과명", example = "컴퓨터공학과") String departmentName,
        @Schema(description = "학과 활성 여부", example = "true") boolean departmentActive,
        @Schema(description = "입학연도", example = "2024") short admissionYear,
        @Schema(description = "최소 전공학점", example = "60") int requiredMajorCredits,
        @Schema(description = "최소 교양학점", example = "30") int requiredGeneralCredits,
        @Schema(description = "최소 총학점", example = "130") int requiredTotalCredits,
        @Schema(description = "등록 시각", example = "2026-08-21T16:00:00") LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-08-21T16:10:00") LocalDateTime updatedAt
) {

    public static GraduationRequirementResponseDTO from(GraduationRequirement requirement) {
        return new GraduationRequirementResponseDTO(
                requirement.getId(),
                requirement.getDepartment().getId(),
                requirement.getDepartment().getCode(),
                requirement.getDepartment().getName(),
                requirement.getDepartment().isActive(),
                requirement.getAdmissionYear(),
                requirement.getRequiredMajorCredits(),
                requirement.getRequiredGeneralCredits(),
                requirement.getRequiredTotalCredits(),
                requirement.getCreatedAt(),
                requirement.getUpdatedAt()
        );
    }
}
