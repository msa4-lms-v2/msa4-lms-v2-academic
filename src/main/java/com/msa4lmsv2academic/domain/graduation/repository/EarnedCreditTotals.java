package com.msa4lmsv2academic.domain.graduation.repository;

public record EarnedCreditTotals(
        int major,
        int general,
        int required,
        int elective,
        int total
) {

    public static EarnedCreditTotals empty() {
        return new EarnedCreditTotals(0, 0, 0, 0, 0);
    }
}
