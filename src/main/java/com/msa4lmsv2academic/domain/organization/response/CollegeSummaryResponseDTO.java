package com.msa4lmsv2academic.domain.organization.response;

import com.msa4lmsv2academic.domain.organization.entity.College;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단과대 요약")
public record CollegeSummaryResponseDTO(
        @Schema(description = "단과대 ID", example = "1")
        Long id,
        @Schema(description = "단과대 고유 코드", example = "ENG", maxLength = 20)
        String code,
        @Schema(description = "단과대명", example = "공과대학", maxLength = 100)
        String name,
        @Schema(description = "활성 여부", example = "true")
        boolean active
) {

    public static CollegeSummaryResponseDTO from(College college) {
        return new CollegeSummaryResponseDTO(
                college.getId(),
                college.getCode(),
                college.getName(),
                college.isActive()
        );
    }
}
