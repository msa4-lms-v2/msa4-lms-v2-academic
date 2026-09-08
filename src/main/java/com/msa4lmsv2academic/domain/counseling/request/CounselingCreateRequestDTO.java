package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "온라인 상담 신청 요청")
public record CounselingCreateRequestDTO(
        @NotNull @Positive
        @Schema(description = "상담을 요청할 교수 ID", example = "31")
        Long professorId,

        @NotBlank @Size(max = 200)
        @Schema(description = "상담 제목", example = "진로 상담 문의")
        String title,

        @NotBlank @Size(max = 10000)
        @Schema(description = "상담 내용")
        String question
) {
}
