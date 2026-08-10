package com.msa4lmsv2academic.graduation.credit.model;

import java.util.Objects;

public record CreditDiagnosisSource(
        GraduationCreditRequirement requirement,
        EarnedCreditSummary earnedCredits
) {

    public CreditDiagnosisSource {
        Objects.requireNonNull(requirement, "requirement must not be null");
        Objects.requireNonNull(earnedCredits, "earnedCredits must not be null");
    }
}
