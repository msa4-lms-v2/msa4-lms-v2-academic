package com.msa4lmsv2academic.domain.organization.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "학과 부분 수정 요청. name, collegeId, active 중 최소 한 필드가 필요합니다.")
public record DepartmentUpdateRequestDTO(
        @Schema(description = "변경할 학과명", example = "AI컴퓨터공학과")
        String name,

        @Schema(description = "변경할 소속 단과대 ID", example = "2")
        @Positive(message = "collegeId는 양수여야 합니다.")
        Long collegeId,

        @Schema(description = "변경할 활성 여부")
        Boolean active
) {

    public boolean hasAnyField() {
        return name != null || collegeId != null || active != null;
    }
}
