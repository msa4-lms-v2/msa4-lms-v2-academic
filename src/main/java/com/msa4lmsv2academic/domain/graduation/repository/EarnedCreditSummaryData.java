package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.graduation.entity.CreditCategory;

import java.util.Map;

public record EarnedCreditSummaryData(
        int totalCredits,
        Map<CreditCategory, Integer> creditsByCategory
) {

    public EarnedCreditSummaryData {
        creditsByCategory = Map.copyOf(creditsByCategory);
    }

    public int creditsOf(CreditCategory category) {
        return creditsByCategory.getOrDefault(category, 0);
    }
}
