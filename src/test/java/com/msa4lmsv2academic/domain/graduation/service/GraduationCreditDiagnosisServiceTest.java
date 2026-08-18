package com.msa4lmsv2academic.domain.graduation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraduationCreditDiagnosisServiceTest {

    @Test
    void diagnosesShortageByDocumentedCreditRequirements() {
        GraduationCreditDiagnosisService service = serviceWithOwner(
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 54, 32, 40, 80, 120)
        );

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(2001L, "STUDENT"));

        assertEquals(6, response.shortageMajorCredits());
        assertEquals(0, response.shortageGeneralCredits());
        assertEquals(10, response.shortageTotalCredits());
        assertFalse(response.satisfied());
    }

    @Test
    void reportsSatisfiedWhenAllDocumentedRequirementsAreMet() {
        GraduationCreditDiagnosisService service = serviceWithOwner(
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132)
        );

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(2001L, "STUDENT"));

        assertTrue(response.satisfied());
        assertEquals(0, response.shortageMajorCredits());
        assertEquals(0, response.shortageGeneralCredits());
        assertEquals(0, response.shortageTotalCredits());
    }

    @Test
    void rejectsInvalidStudentId() {
        GraduationCreditDiagnosisService service = serviceWithOwner(
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 0, 0, 0, 0, 0)
        );

        assertThrows(InvalidCreditDiagnosisRequestException.class,
                () -> service.diagnose(0L, new CurrentUser(2001L, "STUDENT")));
    }

    @Test
    void rejectsMissingCreditDiagnosisData() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(1001L, 2001L)).thenReturn(true);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.empty());
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        assertThrows(GraduationCreditDataNotFoundException.class,
                () -> service.diagnose(1001L, new CurrentUser(2001L, "STUDENT")));
    }

    @Test
    void rejectsStudentWhoRequestsAnotherStudentsDiagnosis() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(1001L, 2001L)).thenReturn(false);
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        assertThrows(GraduationCreditAccessDeniedException.class,
                () -> service.diagnose(1001L, new CurrentUser(2001L, "STUDENT")));
    }

    @Test
    void allowsAdminToDiagnoseManagedStudent() {
        GraduationCreditDiagnosisQueryResult result =
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132);
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(result));
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(3001L, "ADMIN"));

        assertTrue(response.satisfied());
    }

    @Test
    void allowsAdvisorToDiagnoseAssignedStudent() {
        GraduationCreditDiagnosisQueryResult result =
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132);
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentAdvisedByUser(1001L, 4001L)).thenReturn(true);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(result));
        GraduationCreditDiagnosisService service = new GraduationCreditDiagnosisService(repository);

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(4001L, "PROFESSOR"));

        assertTrue(response.satisfied());
    }

    private GraduationCreditDiagnosisService serviceWithOwner(GraduationCreditDiagnosisQueryResult queryResult) {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(1001L, 2001L)).thenReturn(true);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(queryResult));
        return new GraduationCreditDiagnosisService(repository);
    }
}
