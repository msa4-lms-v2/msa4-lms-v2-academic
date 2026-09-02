package com.msa4lmsv2academic.domain.dismissal.request;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "관리자가 실제 근거를 확인한 제적 후보 등록. 학생 자퇴는 별도 API")
public record DismissalCreateRequestDTO(
        @NotNull @Positive @Schema(description = "Academic 학생 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long studentId,
        @NotNull @Schema(description = "제적 사유 종류. 자동 근거 판정 결과가 아님", example = "DISCIPLINARY", requiredMode = Schema.RequiredMode.REQUIRED) DismissalReasonType reasonType,
        @NotBlank @Size(max = 500) @Schema(description = "관리자용 상세 근거(공백 제외 1~500자). 학적 이력에는 원문을 노출하지 않음", example = "징계 심의 결과를 확인했습니다.", requiredMode = Schema.RequiredMode.REQUIRED) String reason
) {
    public DismissalCreateRequestDTO { reason = reason == null ? null : reason.strip(); }
}
