package com.msa4lmsv2academic.domain.graduation.repository;

public record GraduationCreditRequirementData(
        int requiredMajorCredits,
        int requiredGeneralCredits,
        int requiredTotalCredits
) {
}
