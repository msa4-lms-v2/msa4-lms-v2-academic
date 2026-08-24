package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.RetakeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "재수강 조건 판정")
public record RetakeConditionResponseDTO(
        @Schema(description = "재수강 판정 상태", example = "RETAKE_ALLOWED") RetakeStatus status,
        @Schema(description = "재수강 조건 충족 여부. 첫 수강은 true", example = "true") boolean satisfied,
        @Schema(description = "판정에 사용한 성적. 첫 수강은 null", example = "C+", nullable = true)
        String referenceGrade,
        @Schema(description = "미충족 사유. 충족했으면 null", nullable = true)
        PrerequisiteRetakeReasonResponseDTO reason
) {
}
