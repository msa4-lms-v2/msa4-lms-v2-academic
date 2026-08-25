package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;

public record EnrollmentCreditLimitRuleSearchCondition(
        long offset,
        int limit,
        Short academicYear,
        SemesterTerm term,
        Boolean active,
        String sortBy,
        boolean descending
) {
}
