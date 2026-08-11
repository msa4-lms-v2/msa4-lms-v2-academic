package com.msa4lmsv2academic.domain.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "학과 등록 요청")
public record DepartmentCreateRequestDTO(
        @Schema(description = "학과 고유 코드", example = "CSE", maxLength = 20)
        @NotBlank(message = "code는 필수입니다.")
        @Size(max = 20, message = "code는 20자 이하여야 합니다.")
        String code,

        @Schema(description = "학과명", example = "컴퓨터공학과", maxLength = 100)
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "소속 단과대 ID. 소속 단과대가 없으면 생략할 수 있습니다.", example = "1", nullable = true)
        @Positive(message = "collegeId는 양수여야 합니다.")
        Long collegeId,

        @Schema(description = "활성 여부. 생략하면 true입니다.", defaultValue = "true")
        Boolean active
) {
}
