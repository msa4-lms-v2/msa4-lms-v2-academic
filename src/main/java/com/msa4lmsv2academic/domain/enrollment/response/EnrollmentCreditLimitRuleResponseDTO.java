package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "최대 신청학점 규칙 응답")
public record EnrollmentCreditLimitRuleResponseDTO(
        @Schema(description = "규칙 ID", example = "5")
        Long ruleId,

        @Schema(description = "낙관적 락 버전", example = "0")
        long version,

        @Schema(description = "학기 ID", example = "12")
        Long semesterId,

        @Schema(description = "학년도", example = "2027")
        short academicYear,

        @Schema(description = "학기 구분", example = "FIRST", allowableValues = {"FIRST", "SECOND"})
        SemesterTerm term,

        @Schema(description = "수강신청 시작 일시", example = "2027-02-08T09:00:00")
        LocalDateTime enrollmentStartAt,

        @Schema(description = "수강신청 종료 일시", example = "2027-02-12T18:00:00")
        LocalDateTime enrollmentEndAt,

        @Schema(description = "최대 신청학점", example = "18", minimum = "1", maximum = "30")
        int maxCredits,

        @Schema(description = "활성 여부", example = "true")
        boolean active,

        @Schema(description = "등록 시각", example = "2026-08-25T17:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-08-25T17:00:00")
        LocalDateTime updatedAt
) {

    public static EnrollmentCreditLimitRuleResponseDTO from(EnrollmentCreditLimitRule rule) {
        return new EnrollmentCreditLimitRuleResponseDTO(
                rule.getId(),
                rule.getVersion(),
                rule.getSemester().getId(),
                rule.getSemester().getAcademicYear(),
                rule.getSemester().getTerm(),
                rule.getSemester().getEnrollmentStartAt(),
                rule.getSemester().getEnrollmentEndAt(),
                rule.getMaxCredits(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
