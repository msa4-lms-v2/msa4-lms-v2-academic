package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "온라인 상담 교수 답변 등록·수정 요청")
public record OnlineCounselingResponseUpdateRequestDTO(
        @Schema(description = "교수 답변", example = "졸업요건 확인 후 필요한 과목을 안내했습니다.")
        @NotBlank(message = "response는 필수입니다.")
        @Size(max = 5000, message = "response는 5000자 이하여야 합니다.")
        String response
) {
}
