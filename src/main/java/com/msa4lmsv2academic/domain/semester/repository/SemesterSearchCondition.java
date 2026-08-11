package com.msa4lmsv2academic.domain.semester.repository;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;

public record SemesterSearchCondition(
        long offset,
        int limit,
        Short academicYear,
        SemesterTerm term,
        Boolean current
) {
}
