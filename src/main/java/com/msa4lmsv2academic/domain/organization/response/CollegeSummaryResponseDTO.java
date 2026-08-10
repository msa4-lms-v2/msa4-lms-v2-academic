package com.msa4lmsv2academic.domain.organization.response;

import com.msa4lmsv2academic.domain.organization.entity.College;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단과대 요약")
public record CollegeSummaryResponseDTO(
        Long id,
        String code,
        String name,
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
