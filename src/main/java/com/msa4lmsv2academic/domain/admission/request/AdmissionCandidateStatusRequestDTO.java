package com.msa4lmsv2academic.domain.admission.request;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "입학 예정자 상태 변경 요청")
public record AdmissionCandidateStatusRequestDTO(
        @Schema(description = "관리자가 요청할 상태. CONFIRMED 또는 CANCELLED만 허용",
                example = "CONFIRMED", allowableValues = {"CONFIRMED", "CANCELLED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "status는 필수입니다.")
        AdmissionCandidateStatus status,

        @Schema(description = "상태 변경 사유", example = "2027학년도 최종 등록 대상자 확정",
                minLength = 1, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {
}
