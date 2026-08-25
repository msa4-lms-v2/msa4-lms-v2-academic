package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import java.util.List;

public record EnrollmentCreditLimitRuleSearchResult(
        List<EnrollmentCreditLimitRule> items,
        long totalCount
) {
}
