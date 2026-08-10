package com.msa4lmsv2academic.graduation.credit.application;

import com.msa4lmsv2academic.graduation.credit.model.CreditCategory;
import com.msa4lmsv2academic.graduation.credit.model.CreditDiagnosisResult;
import com.msa4lmsv2academic.graduation.credit.model.CreditDiagnosisSource;
import com.msa4lmsv2academic.graduation.credit.model.EarnedCreditSummary;
import com.msa4lmsv2academic.graduation.credit.model.GraduationCreditRequirement;
import java.util.Objects;

public final class GraduationCreditDiagnosisService {

    private final CreditDiagnosisDataProvider dataProvider;

    public GraduationCreditDiagnosisService(CreditDiagnosisDataProvider dataProvider) {
        this.dataProvider = Objects.requireNonNull(dataProvider, "dataProvider must not be null");
    }

    public CreditDiagnosisResult diagnose(long studentId) {
        if (studentId <= 0) {
            throw new IllegalArgumentException("studentId must be positive");
        }

        CreditDiagnosisSource source = Objects.requireNonNull(
                dataProvider.load(studentId),
                "credit diagnosis source must not be null"
        );
        GraduationCreditRequirement requirement = source.requirement();
        EarnedCreditSummary earned = source.earnedCredits();

        int majorShortage = shortage(
                requirement.requiredMajorCredits(),
                earned.creditsOf(CreditCategory.MAJOR)
        );
        int generalShortage = shortage(
                requirement.requiredGeneralCredits(),
                earned.creditsOf(CreditCategory.GENERAL)
        );
        int totalShortage = shortage(requirement.requiredTotalCredits(), earned.totalCredits());

        return new CreditDiagnosisResult(
                studentId,
                earned.totalCredits(),
                earned.creditsByCategory(),
                majorShortage,
                generalShortage,
                totalShortage,
                majorShortage == 0 && generalShortage == 0 && totalShortage == 0
        );
    }

    private int shortage(int requiredCredits, int earnedCredits) {
        return Math.max(requiredCredits - earnedCredits, 0);
    }
}
