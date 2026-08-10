package com.msa4lmsv2academic.domain.graduation.repository;

public record GraduationCreditDiagnosisQueryResult(
        int requiredMajorCredits,
        int requiredGeneralCredits,
        int requiredTotalCredits,
        int earnedMajorCredits,
        int earnedGeneralCredits,
        int earnedRequiredCredits,
        int earnedElectiveCredits,
        int earnedTotalCredits
) {
}
