package com.msa4lmsv2academic.domain.organization.response;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학과 응답")
public record DepartmentResponseDTO(
        @Schema(description = "학과 ID", example = "1")
        Long id,
        @Schema(description = "학과 고유 코드", example = "CSE", maxLength = 20)
        String code,
        @Schema(description = "학과명", example = "컴퓨터공학과", maxLength = 100)
        String name,
        @Schema(description = "활성 여부", example = "true")
        boolean active,
        @Schema(description = "소속 단과대 요약. 소속 단과대가 없으면 null입니다.", nullable = true)
        CollegeSummaryResponseDTO college
) {

    public static DepartmentResponseDTO from(Department department) {
        return new DepartmentResponseDTO(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.isActive(),
                department.getCollege() == null ? null : CollegeSummaryResponseDTO.from(department.getCollege())
        );
    }
}
