package com.msa4lmsv2academic.graduation.credit.model;

import java.util.Map;
import java.util.Objects;

public record EarnedCreditSummary(
        int totalCredits,
        Map<CreditCategory, Integer> creditsByCategory
) {

    public EarnedCreditSummary {
        if (totalCredits < 0) {
            throw new IllegalArgumentException("totalCredits must not be negative");
        }

        Objects.requireNonNull(creditsByCategory, "creditsByCategory must not be null");
        if (creditsByCategory.values().stream().anyMatch(credits -> credits == null || credits < 0)) {
            throw new IllegalArgumentException("category credits must not be null or negative");
        }

        creditsByCategory = Map.copyOf(creditsByCategory);
    }

    public int creditsOf(CreditCategory category) {
        return creditsByCategory.getOrDefault(category, 0);
    }
}
