package com.msa4lmsv2academic.domain.transfer.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAcademicChangeRejectionRequestDTO(
        @Schema(description = "학장 날인 누락에 따른 관리자 반려 사유", example = "두 제출 서류에 학장 날인이 확인되지 않습니다.")
        @NotBlank @Size(max = 500) String rejectReason
) {
}
