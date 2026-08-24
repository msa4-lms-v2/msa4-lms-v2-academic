package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleCriteriaResponseDTO;

public record PrerequisiteRetakeRuleCreateResult(
        PrerequisiteRetakeRuleCriteriaResponseDTO response,
        boolean created
) {
}
