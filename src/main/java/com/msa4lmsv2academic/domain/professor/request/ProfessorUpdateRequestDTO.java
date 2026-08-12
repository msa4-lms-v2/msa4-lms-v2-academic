package com.msa4lmsv2academic.domain.professor.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 교수 인사정보 부분 수정 요청")
public record ProfessorUpdateRequestDTO(
        @Schema(description = "변경할 활성 학과 ID. 생략하거나 null이면 기존 값 유지", example = "3")
        @Positive(message = "departmentId는 양수여야 합니다.")
        Long departmentId,

        @Schema(description = "변경할 임용 연도. 생략하거나 null이면 기존 값 유지", example = "2020", minimum = "1900")
        @Min(value = 1900, message = "hireYear는 1900 이상이어야 합니다.")
        Integer hireYear,

        @Schema(
                description = "관리자 변경 사유",
                example = "소속 학과 변경",
                minLength = 1,
                maxLength = 255,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "reason은 필수입니다.")
        @Size(max = 255, message = "reason은 255자 이하여야 합니다.")
        String reason
) {

    public boolean hasAnyUpdateField() {
        return departmentId != null || hireYear != null;
    }
}
