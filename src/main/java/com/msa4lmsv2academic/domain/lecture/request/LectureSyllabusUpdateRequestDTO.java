package com.msa4lmsv2academic.domain.lecture.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "강의계획서 작성·수정 요청")
public record LectureSyllabusUpdateRequestDTO(
        @Schema(
                description = "강의계획서 본문",
                example = "운영체제의 핵심 개념과 주차별 실습 계획을 학습합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "강의계획서는 필수입니다.")
        @Size(max = 65535, message = "강의계획서는 65535자 이하여야 합니다.")
        String syllabus
) {
}
