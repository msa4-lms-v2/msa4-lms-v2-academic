package com.msa4lmsv2academic.domain.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

@Schema(description = "학과 부분 수정 요청. name, active 중 최소 한 필드가 필요합니다.")
public record DepartmentUpdateRequestDTO(
        @Schema(description = "변경할 학과명", example = "AI컴퓨터공학과", maxLength = 100)
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "변경할 활성 여부")
        Boolean active
) {

    @AssertTrue(message = "name, active 중 최소 한 필드가 필요합니다.")
    @Schema(hidden = true)
    public boolean hasAnyField() {
        return name != null || active != null;
    }
}
