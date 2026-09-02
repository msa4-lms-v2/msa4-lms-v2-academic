package com.msa4lmsv2academic.domain.enrollment.response;

import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선수과목·재수강 조건 미충족 사유")
public record PrerequisiteRetakeReasonResponseDTO(
        @Schema(description = "표준 사유 코드", example = "PREREQUISITE_NOT_COMPLETED")
        PrerequisiteRetakeRuleRejectionReason code,
        @Schema(description = "사용자 안내 메시지", example = "선수과목을 이수하지 않았습니다.")
        String message
) {

    public static PrerequisiteRetakeReasonResponseDTO from(
            PrerequisiteRetakeRuleRejectionReason reason
    ) {
        return new PrerequisiteRetakeReasonResponseDTO(reason, reason.getMessage());
    }
}
