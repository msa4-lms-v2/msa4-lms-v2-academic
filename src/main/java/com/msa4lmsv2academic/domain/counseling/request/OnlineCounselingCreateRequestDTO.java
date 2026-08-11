package com.msa4lmsv2academic.domain.counseling.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "학생 온라인 상담 신청 요청")
public record OnlineCounselingCreateRequestDTO(
        @Schema(description = "상담 제목", example = "졸업 요건 상담")
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 150, message = "title은 150자 이하여야 합니다.")
        String title,

        @Schema(description = "학생 상담 내용", example = "졸업에 필요한 전공필수 학점을 확인하고 싶습니다.")
        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        String content
) {
}
