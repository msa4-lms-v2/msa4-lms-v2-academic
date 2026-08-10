package com.msa4lmsv2academic.domain.graduation.service;

public record GraduationCreditDiagnosisResult(
        Long studentId,
        int earnedMajorCredits,
        int earnedGeneralCredits,
        int earnedRequiredCredits,
        int earnedElectiveCredits,
        int earnedTotalCredits,
        int shortageMajorCredits,
        int shortageGeneralCredits,
        int shortageTotalCredits,
        boolean satisfied
) {
}
