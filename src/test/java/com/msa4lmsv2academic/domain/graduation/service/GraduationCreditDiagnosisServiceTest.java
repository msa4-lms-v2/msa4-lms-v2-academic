package com.msa4lmsv2academic.domain.graduation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.msa4lmsv2academic.domain.graduation.entity.CreditCategory;
import com.msa4lmsv2academic.domain.graduation.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.domain.graduation.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.domain.graduation.repository.EarnedCreditSummaryData;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisData;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditRequirementData;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraduationCreditDiagnosisServiceTest {

    @Test
    void diagnosesShortageByDocumentedCreditRequirements() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirementData(60, 30, 130),
                new EarnedCreditSummaryData(
                        120,
                        Map.of(
                                CreditCategory.MAJOR, 54,
                                CreditCategory.GENERAL, 32,
                                CreditCategory.REQUIRED, 40,
                                CreditCategory.ELECTIVE, 80
                        )
                )
        );

        CreditDiagnosisResponseDTO response = service.diagnose(1001L);

        assertEquals(6, response.shortageMajorCredits());
        assertEquals(0, response.shortageGeneralCredits());
        assertEquals(10, response.shortageTotalCredits());
        assertFalse(response.satisfied());
    }

    @Test
    void reportsSatisfiedWhenAllDocumentedRequirementsAreMet() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirementData(60, 30, 130),
                new EarnedCreditSummaryData(
                        132,
                        Map.of(
                                CreditCategory.MAJOR, 62,
                                CreditCategory.GENERAL, 34,
                                CreditCategory.REQUIRED, 45,
                                CreditCategory.ELECTIVE, 87
                        )
                )
        );

        CreditDiagnosisResponseDTO response = service.diagnose(1001L);

        assertTrue(response.satisfied());
        assertEquals(0, response.shortageMajorCredits());
        assertEquals(0, response.shortageGeneralCredits());
        assertEquals(0, response.shortageTotalCredits());
    }

    @Test
    void rejectsInvalidStudentId() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditRequirementData(60, 30, 130),
                new EarnedCreditSummaryData(0, Map.of())
        );

        assertThrows(InvalidCreditDiagnosisRequestException.class, () -> service.diagnose(0L));
    }

    @Test
    void rejectsMissingCreditDiagnosisData() {
        GraduationCreditQueryRepository repository = studentId -> Optional.empty();
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        assertThrows(GraduationCreditDataNotFoundException.class, () -> service.diagnose(1001L));
    }

    private GraduationCreditDiagnosisService serviceWith(
            GraduationCreditRequirementData requirement,
            EarnedCreditSummaryData earnedCredits
    ) {
        GraduationCreditDiagnosisData data = new GraduationCreditDiagnosisData(requirement, earnedCredits);
        GraduationCreditQueryRepository repository = studentId -> Optional.of(data);
        return new GraduationCreditDiagnosisService(repository);
    }
}
