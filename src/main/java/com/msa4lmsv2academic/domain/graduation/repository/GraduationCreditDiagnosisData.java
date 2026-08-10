package com.msa4lmsv2academic.domain.graduation.repository;

public record GraduationCreditDiagnosisData(
        GraduationCreditRequirementData requirement,
        EarnedCreditSummaryData earnedCredits
) {
}
