package com.msa4lmsv2academic.domain.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "학과 등록 요청")
public record DepartmentCreateRequestDTO(
        @Schema(description = "학과 코드. 영문, 숫자, 하이픈만 허용하며 대문자로 저장됩니다.", example = "CSE")
        @NotBlank(message = "code는 필수입니다.")
        String code,

        @Schema(description = "학과명", example = "컴퓨터공학과")
        @NotBlank(message = "name은 필수입니다.")
        String name,

        @Schema(description = "소속 단과대 ID", example = "1")
        @NotNull(message = "collegeId는 필수입니다.")
        @Positive(message = "collegeId는 양수여야 합니다.")
        Long collegeId,

        @Schema(description = "활성 여부. 생략하면 true입니다.", defaultValue = "true")
        Boolean active
) {
}
