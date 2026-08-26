package com.msa4lmsv2academic.domain.enrollment.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "수강신청 POST 업무 거절 시에만 반환하는 data. 기존 API 오류 응답은 변경하지 않습니다.")
public record EnrollmentApplicationErrorResponseDTO(
        @Schema(description = "검증 중 확인한 거절 사유. 모든 검증 실패를 일괄 수집하는 것은 아닙니다.",
                requiredMode = Schema.RequiredMode.REQUIRED) List<EnrollmentApplicationReasonResponseDTO> reasons
) {
    public EnrollmentApplicationErrorResponseDTO {
        reasons = List.copyOf(reasons);
    }

    public static EnrollmentApplicationErrorResponseDTO from(List<EnrollmentApplicationReasonResponseDTO> reasons) {
        return new EnrollmentApplicationErrorResponseDTO(reasons);
    }
}
