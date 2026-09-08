package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "온라인 상담 답변 요청")
public record CounselingAnswerRequestDTO(
        @NotBlank @Size(max = 10000)
        @Schema(description = "교수 답변")
        String answer
) {
}
