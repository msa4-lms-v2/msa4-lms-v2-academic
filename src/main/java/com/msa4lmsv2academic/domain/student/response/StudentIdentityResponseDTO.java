package com.msa4lmsv2academic.domain.student.response;

public record StudentIdentityResponseDTO(
        Long studentId,
        String studentNumber,
        String name,
        String departmentName
) {
}
