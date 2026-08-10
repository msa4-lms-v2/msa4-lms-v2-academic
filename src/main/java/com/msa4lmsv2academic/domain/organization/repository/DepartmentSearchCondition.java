package com.msa4lmsv2academic.domain.organization.repository;

public record DepartmentSearchCondition(
        long offset,
        int limit,
        Long collegeId,
        Boolean active,
        String keyword,
        boolean admin
) {
}
