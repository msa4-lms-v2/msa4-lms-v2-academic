package com.msa4lmsv2academic.graduation.credit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.msa4lmsv2academic.graduation.credit.model.CreditCategory;
import com.msa4lmsv2academic.graduation.credit.model.CreditDiagnosisResult;
import com.msa4lmsv2academic.graduation.credit.model.CreditDiagnosisSource;
import com.msa4lmsv2academic.graduation.credit.model.EarnedCreditSummary;
import com.msa4lmsv2academic.graduation.credit.model.GraduationCreditRequirement;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraduationCreditDiagnosisServiceTest {

    @Test
    void diagnosesShortageByDocumentedCreditRequirements() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirement(60, 30, 130),
                new EarnedCreditSummary(
                        120,
                        Map.of(
                                CreditCategory.MAJOR, 54,
                                CreditCategory.GENERAL, 32,
                                CreditCategory.REQUIRED, 40,
                                CreditCategory.ELECTIVE, 80
                        )
                )
        );

        CreditDiagnosisResult result = service.diagnose(1001L);

        assertEquals(6, result.shortageMajorCredits());
        assertEquals(0, result.shortageGeneralCredits());
        assertEquals(10, result.shortageTotalCredits());
        assertFalse(result.satisfied());
    }

    @Test
    void reportsSatisfiedWhenAllDocumentedRequirementsAreMet() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirement(60, 30, 130),
                new EarnedCreditSummary(
                        132,
                        Map.of(
                                CreditCategory.MAJOR, 62,
                                CreditCategory.GENERAL, 34,
                                CreditCategory.REQUIRED, 45,
                                CreditCategory.ELECTIVE, 87
                        )
                )
        );

        CreditDiagnosisResult result = service.diagnose(1001L);

        assertTrue(result.satisfied());
        assertEquals(0, result.shortageMajorCredits());
        assertEquals(0, result.shortageGeneralCredits());
        assertEquals(0, result.shortageTotalCredits());
    }

    @Test
    void rejectsInvalidStudentId() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirement(60, 30, 130),
                new EarnedCreditSummary(0, Map.of())
        );

        assertThrows(IllegalArgumentException.class, () -> service.diagnose(0));
    }

    private GraduationCreditDiagnosisService serviceWith(
            GraduationCreditRequirement requirement,
            EarnedCreditSummary earnedCredits
    ) {
        CreditDiagnosisSource source = new CreditDiagnosisSource(requirement, earnedCredits);
        return new GraduationCreditDiagnosisService(studentId -> source);
    }
}
