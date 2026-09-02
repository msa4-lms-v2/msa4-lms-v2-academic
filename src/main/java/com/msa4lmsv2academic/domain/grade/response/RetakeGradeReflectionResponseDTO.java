package com.msa4lmsv2academic.domain.grade.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "재수강 성적 반영 결과")
public record RetakeGradeReflectionResponseDTO(
        @Schema(description = "반영된 수강 ID", example = "302") Long enrollmentId,
        @Schema(description = "학생 ID", example = "41") Long studentId,
        @Schema(description = "교과목 ID", example = "17") Long courseId,
        @Schema(description = "이전 수강 ID", example = "145") Long previousEnrollmentId,
        @Schema(description = "이전 성적", example = "C+") String previousGrade,
        @Schema(description = "새로 반영된 성적", example = "A") String reflectedGrade,
        @Schema(description = "처리자 Academic 사용자 ID", example = "3") Long processedBy,
        @Schema(description = "처리시각", example = "2026-08-28T14:30:00") LocalDateTime processedAt,
        @Schema(description = "재계산된 학기별 성적 요약") List<StudentGradeSummaryResponseDTO> summaries
) {
    public RetakeGradeReflectionResponseDTO {
        summaries = List.copyOf(summaries);
    }
}
