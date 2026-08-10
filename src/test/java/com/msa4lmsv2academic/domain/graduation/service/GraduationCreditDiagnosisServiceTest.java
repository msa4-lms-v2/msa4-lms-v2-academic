package com.msa4lmsv2academic.domain.graduation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraduationCreditDiagnosisServiceTest {

    @Test
    void diagnosesShortageByDocumentedCreditRequirements() {
        GraduationCreditDiagnosisService service = serviceWith(
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 54, 32, 40, 80, 120)
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
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132)
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
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 0, 0, 0, 0, 0)
        );

        assertThrows(InvalidCreditDiagnosisRequestException.class, () -> service.diagnose(0L));
    }

    @Test
    void rejectsMissingCreditDiagnosisData() {
        GraduationCreditQueryRepository repository = studentId -> Optional.empty();
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        assertThrows(GraduationCreditDataNotFoundException.class, () -> service.diagnose(1001L));
    }

    private GraduationCreditDiagnosisService serviceWith(GraduationCreditDiagnosisQueryResult queryResult) {
        GraduationCreditQueryRepository repository = studentId -> Optional.of(queryResult);
        return new GraduationCreditDiagnosisService(repository);
    }
}
