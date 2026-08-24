package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.global.response.PageResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선수과목 기준정보와 선택적 개인별 판정 응답")
public record PrerequisiteRetakeRuleQueryResponseDTO(
        @Schema(description = "권한·필터에 맞는 선수과목 기준 목록")
        PageResponseDTO<PrerequisiteRetakeRuleCriteriaResponseDTO> criteria,
        @Schema(description = "개인별 판정 결과. ADMIN 기준정보 목록 조회는 null", nullable = true)
        PrerequisiteRetakeEvaluationResponseDTO evaluation
) {
}
