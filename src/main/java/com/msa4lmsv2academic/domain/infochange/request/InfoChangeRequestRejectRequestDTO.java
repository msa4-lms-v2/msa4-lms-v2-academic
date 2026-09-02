package com.msa4lmsv2academic.domain.infochange.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "학적 정보 변경 신청 반려 요청")
public record InfoChangeRequestRejectRequestDTO(
        @Schema(description = "반려 사유", example = "증빙 내용이 충분하지 않습니다.", maxLength = 500)
        @NotBlank
        @Size(max = 500)
        String rejectReason
) {
}
