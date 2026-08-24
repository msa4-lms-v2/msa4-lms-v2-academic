package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import java.util.List;

public record PrerequisiteRetakeRuleSearchResult(
        List<CoursePrerequisite> items,
        long totalCount
) {
}
