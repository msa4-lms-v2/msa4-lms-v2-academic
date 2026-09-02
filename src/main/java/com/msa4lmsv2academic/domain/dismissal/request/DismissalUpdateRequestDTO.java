package com.msa4lmsv2academic.domain.dismissal.request;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "대기 후보의 수정 가능한 내용 전체. 학생 및 최초 등록자는 변경 불가")
public record DismissalUpdateRequestDTO(
        @NotNull @PositiveOrZero @Schema(description = "관리자가 조회한 최신 version", example = "0", requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @NotNull @Schema(description = "변경할 제적 사유 종류", example = "DISCIPLINARY", requiredMode = Schema.RequiredMode.REQUIRED) DismissalReasonType reasonType,
        @NotBlank @Size(max = 500) @Schema(description = "변경할 상세 근거(1~500자)", example = "확인한 처분 근거를 보완합니다.", requiredMode = Schema.RequiredMode.REQUIRED) String reason
) {
    public DismissalUpdateRequestDTO { reason = reason == null ? null : reason.strip(); }
}
