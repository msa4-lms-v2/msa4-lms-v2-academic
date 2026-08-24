package com.msa4lmsv2academic.domain.enrollment.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개별 선수과목 이수 판정")
public record PrerequisiteCompletionResponseDTO(
        @Schema(description = "선수과목 기준 ID", example = "4") Long ruleId,
        @Schema(description = "선수 교과목 ID", example = "10") Long prerequisiteCourseId,
        @Schema(description = "선수 교과목 코드", example = "CSE2001") String prerequisiteCourseCode,
        @Schema(description = "선수 교과목명", example = "자료구조") String prerequisiteCourseName,
        @Schema(description = "이수 충족 여부", example = "true") boolean satisfied,
        @Schema(description = "이수로 인정한 최근 공개 성적. 미이수는 null", example = "B+", nullable = true)
        String completedGrade,
        @Schema(description = "미충족 사유. 충족했으면 null", nullable = true)
        PrerequisiteRetakeReasonResponseDTO reason
) {
}
