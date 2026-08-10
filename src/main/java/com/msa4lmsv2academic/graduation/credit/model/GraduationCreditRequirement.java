package com.msa4lmsv2academic.graduation.credit.model;

public record GraduationCreditRequirement(
        int requiredMajorCredits,
        int requiredGeneralCredits,
        int requiredTotalCredits
) {

    public GraduationCreditRequirement {
        if (requiredMajorCredits < 0 || requiredGeneralCredits < 0 || requiredTotalCredits < 0) {
            throw new IllegalArgumentException("required credits must not be negative");
        }
    }
}
