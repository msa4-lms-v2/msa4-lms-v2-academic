package com.msa4lmsv2academic.domain.grade.response;

import com.msa4lmsv2academic.domain.grade.entity.StudentGradeSummary;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "재수강 반영 후 학기별 성적 요약")
public record StudentGradeSummaryResponseDTO(
        @Schema(description = "학기 ID", example = "21") Long semesterId,
        @Schema(description = "학년도", example = "2026") short academicYear,
        @Schema(description = "학기", example = "FIRST") SemesterTerm term,
        @Schema(description = "최종 반영된 신청 학점", example = "18") short totalCredits,
        @Schema(description = "최종 반영 평점", example = "3.75") BigDecimal gpa
) {
    public static StudentGradeSummaryResponseDTO from(StudentGradeSummary summary) {
        return new StudentGradeSummaryResponseDTO(
                summary.getSemester().getId(),
                summary.getSemester().getAcademicYear(),
                summary.getSemester().getTerm(),
                summary.getTotalCredits(),
                summary.getGpa()
        );
    }
}
