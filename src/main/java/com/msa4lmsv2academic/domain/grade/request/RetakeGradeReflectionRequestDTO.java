package com.msa4lmsv2academic.domain.grade.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "재수강 성적 반영 요청")
public record RetakeGradeReflectionRequestDTO(
        @Schema(description = "반영 사유", example = "2026학년도 1학기 재수강 확정 성적 반영",
                minLength = 1, maxLength = 500)
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 500, message = "reason은 500자 이하여야 합니다.")
        String reason
) {
    public RetakeGradeReflectionRequestDTO {
        reason = reason == null ? null : reason.strip();
    }
}
