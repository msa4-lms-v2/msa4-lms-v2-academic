package com.msa4lmsv2academic.domain.organization.response;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학과 응답")
public record DepartmentResponseDTO(
        Long id,
        String code,
        String name,
        boolean active,
        CollegeSummaryResponseDTO college
) {

    public static DepartmentResponseDTO from(Department department) {
        return new DepartmentResponseDTO(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.isActive(),
                CollegeSummaryResponseDTO.from(department.getCollege())
        );
    }
}
