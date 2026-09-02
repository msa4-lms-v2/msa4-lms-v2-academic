package com.msa4lmsv2academic.domain.graduation.repository;

public record GraduationRequirementSearchCondition(
        long offset,
        int limit,
        String keyword,
        Long departmentId,
        Short admissionYear,
        String sortBy,
        boolean descending
) {
}
