package com.msa4lmsv2academic.domain.graduation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.graduation.entity.CreditDiagnosisStatus;
import com.msa4lmsv2academic.domain.graduation.repository.CreditDiagnosisCandidateRow;
import com.msa4lmsv2academic.domain.graduation.repository.CreditDiagnosisSearchCondition;
import com.msa4lmsv2academic.domain.graduation.repository.EarnedCreditTotals;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.request.CreditDiagnosisSearchRequestDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisSummaryResponseDTO;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.global.error.GraduationCreditAccessDeniedException;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        GraduationCreditDiagnosisService service = service(repository);

        assertThrows(GraduationCreditDataNotFoundException.class,
                () -> service.diagnose(1001L, new CurrentUser(2001L, "STUDENT")));
    }

    @Test
    void rejectsStudentWhoRequestsAnotherStudentsDiagnosis() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(1001L, 2001L)).thenReturn(false);
        GraduationCreditDiagnosisService service = service(repository);

        assertThrows(GraduationCreditAccessDeniedException.class,
                () -> service.diagnose(1001L, new CurrentUser(2001L, "STUDENT")));
    }

    @Test
    void allowsAdminToDiagnoseManagedStudent() {
        GraduationCreditDiagnosisQueryResult result =
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132);
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(result));
        GraduationCreditDiagnosisService service = service(repository);

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(3001L, "ADMIN"));

        assertTrue(response.satisfied());
    }

    @Test
    void allowsAdvisorToDiagnoseAssignedStudent() {
        GraduationCreditDiagnosisQueryResult result =
                new GraduationCreditDiagnosisQueryResult(60, 30, 130, 62, 34, 45, 87, 132);
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        StudentQueryRepository studentQueryRepository = mock(StudentQueryRepository.class);
        ProfessorStudentScope scope = new ProfessorStudentScope(5001L, 6001L);
        when(studentQueryRepository.findProfessorScopeByUserId(4001L)).thenReturn(Optional.of(scope));
        when(repository.isStudentInProfessorScope(1001L, scope)).thenReturn(true);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(result));
        GraduationCreditDiagnosisService service =
                new GraduationCreditDiagnosisService(repository, studentQueryRepository);

        CreditDiagnosisResponseDTO response = service.diagnose(1001L, new CurrentUser(4001L, "PROFESSOR"));

        assertTrue(response.satisfied());
    }

    @Test
    void studentListIsLimitedToOwnerAndReportsMissingRequirement() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        StudentQueryRepository studentQueryRepository = mock(StudentQueryRepository.class);
        CreditDiagnosisCandidateRow candidate = candidate(1001L, null, null, null, null);
        when(repository.findDiagnosisCandidates(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(true))).thenReturn(List.of(candidate));
        when(repository.countDiagnosisCandidates(org.mockito.ArgumentMatchers.any())).thenReturn(1L);
        when(repository.findEarnedCreditsByStudentIds(List.of(1001L)))
                .thenReturn(Map.of(1001L, new EarnedCreditTotals(12, 6, 9, 9, 18)));
        GraduationCreditDiagnosisService service =
                new GraduationCreditDiagnosisService(repository, studentQueryRepository);

        PageResponseDTO<CreditDiagnosisSummaryResponseDTO> response = service.search(
                searchRequest(null, null, null),
                new CurrentUser(2001L, "STUDENT")
        );

        assertEquals(1, response.totalCount());
        CreditDiagnosisSummaryResponseDTO item = response.items().getFirst();
        assertEquals(CreditDiagnosisStatus.REQUIREMENT_NOT_CONFIGURED, item.diagnosisStatus());
        assertEquals(18, item.earnedTotalCredits());
        assertTrue(item.reason().contains("졸업요건이 등록되지 않았습니다"));
        ArgumentCaptor<CreditDiagnosisSearchCondition> conditionCaptor =
                ArgumentCaptor.forClass(CreditDiagnosisSearchCondition.class);
        verify(repository).findDiagnosisCandidates(conditionCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(true));
        assertEquals(2001L, conditionCaptor.getValue().studentUserId());
    }

    @Test
    void diagnosisStatusFilterIsAppliedAfterComputedCredits() {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        StudentQueryRepository studentQueryRepository = mock(StudentQueryRepository.class);
        List<CreditDiagnosisCandidateRow> candidates = List.of(
                candidate(1001L, 1L, 60, 30, 130),
                candidate(1002L, 2L, 60, 30, 130)
        );
        when(repository.findDiagnosisCandidates(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(false))).thenReturn(candidates);
        when(repository.findEarnedCreditsByStudentIds(List.of(1001L, 1002L))).thenReturn(Map.of(
                1001L, new EarnedCreditTotals(60, 30, 50, 40, 130),
                1002L, new EarnedCreditTotals(30, 10, 20, 20, 40)
        ));
        GraduationCreditDiagnosisService service =
                new GraduationCreditDiagnosisService(repository, studentQueryRepository);

        PageResponseDTO<CreditDiagnosisSummaryResponseDTO> response = service.search(
                searchRequest(CreditDiagnosisStatus.SATISFIED, 1, 20),
                new CurrentUser(3001L, "ADMIN")
        );

        assertEquals(1, response.totalCount());
        assertEquals(1001L, response.items().getFirst().studentId());
        assertEquals(CreditDiagnosisStatus.SATISFIED, response.items().getFirst().diagnosisStatus());
    }

    private GraduationCreditDiagnosisService serviceWithOwner(GraduationCreditDiagnosisQueryResult queryResult) {
        GraduationCreditQueryRepository repository = mock(GraduationCreditQueryRepository.class);
        when(repository.isStudentOwnedByUser(1001L, 2001L)).thenReturn(true);
        when(repository.findCreditDiagnosisByStudentId(1001L)).thenReturn(Optional.of(queryResult));
        return service(repository);
    }

    private GraduationCreditDiagnosisService service(GraduationCreditQueryRepository repository) {
        return new GraduationCreditDiagnosisService(repository, mock(StudentQueryRepository.class));
    }

    private CreditDiagnosisCandidateRow candidate(
            Long studentId,
            Long requirementId,
            Integer requiredMajor,
            Integer requiredGeneral,
            Integer requiredTotal
    ) {
        return new CreditDiagnosisCandidateRow(
                studentId,
                "학생" + studentId,
                10L,
                "컴퓨터공학과",
                (short) 2024,
                AcademicStatus.ENROLLED,
                requirementId,
                requiredMajor,
                requiredGeneral,
                requiredTotal
        );
    }

    private CreditDiagnosisSearchRequestDTO searchRequest(
            CreditDiagnosisStatus diagnosisStatus,
            Integer page,
            Integer size
    ) {
        return new CreditDiagnosisSearchRequestDTO(
                page,
                size,
                null,
                null,
                null,
                null,
                diagnosisStatus,
                null,
                null
        );
    }
}
