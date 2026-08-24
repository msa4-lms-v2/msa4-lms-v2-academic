package com.msa4lmsv2academic.domain.enrollment.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "학생·교과목별 선수과목·재수강 조건 판정")
public record PrerequisiteRetakeEvaluationResponseDTO(
        @Schema(description = "판정 대상 학생 ID", example = "8") Long studentId,
        @Schema(description = "대상 교과목 ID", example = "20") Long courseId,
        @Schema(description = "대상 교과목 코드", example = "CSE3001") String courseCode,
        @Schema(description = "대상 교과목명", example = "운영체제") String courseName,
        @Schema(description = "모든 직접 선수과목 이수 여부", example = "false") boolean prerequisiteSatisfied,
        @Schema(description = "직접 선수과목별 이수 판정")
        List<PrerequisiteCompletionResponseDTO> prerequisites,
        @Schema(description = "재수강 조건 판정") RetakeConditionResponseDTO retakeCondition,
        @Schema(description = "선수과목과 재수강 조건을 모두 충족했는지. 전체 수강 가능 여부는 아님",
                example = "false")
        boolean ruleSatisfied,
        @Schema(description = "중복을 제거한 표준 미충족 사유")
        List<PrerequisiteRetakeReasonResponseDTO> reasons
) {
}
