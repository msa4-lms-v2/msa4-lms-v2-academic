package com.msa4lmsv2academic.domain.enrollment.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수강신청 거절 사유")
public record EnrollmentApplicationReasonResponseDTO(
        @Schema(description = "안정적인 업무 거절 코드", example = "CREDIT_LIMIT_EXCEEDED",
                requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(description = "거절 사유 설명", example = "최대 신청학점을 초과합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED) String message
) {
    public static EnrollmentApplicationReasonResponseDTO from(String code, String message) {
        return new EnrollmentApplicationReasonResponseDTO(code, message);
    }
}
