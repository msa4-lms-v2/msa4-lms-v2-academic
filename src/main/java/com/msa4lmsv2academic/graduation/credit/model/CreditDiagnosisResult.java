package com.msa4lmsv2academic.graduation.credit.model;

import java.util.Map;

public record CreditDiagnosisResult(
        long studentId,
        int earnedTotalCredits,
        Map<CreditCategory, Integer> earnedCreditsByCategory,
        int shortageMajorCredits,
        int shortageGeneralCredits,
        int shortageTotalCredits,
        boolean satisfied
) {

    public CreditDiagnosisResult {
        earnedCreditsByCategory = Map.copyOf(earnedCreditsByCategory);
    }
}
