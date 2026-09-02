package com.msa4lmsv2academic.domain.professor.repository;

import com.msa4lmsv2academic.domain.user.entity.UserStatus;

public record ProfessorSearchCondition(
        long offset,
        long limit,
        Long departmentId,
        Short hireYear,
        UserStatus status,
        String keyword
) {
}
