package com.msa4lmsv2academic.domain.enrollment.repository;

public record PrerequisiteRetakeRuleSearchCondition(
        long offset,
        int limit,
        String keyword,
        Long courseId,
        Boolean active,
        String sortBy,
        boolean descending
) {
}
