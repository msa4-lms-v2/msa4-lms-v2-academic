package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationCreditDiagnosisService {

    private final GraduationCreditQueryRepository graduationCreditQueryRepository;

    public CreditDiagnosisResponseDTO diagnose(Long studentId, CurrentUser currentUser) {
        if (studentId == null || studentId <= 0) {
            throw new InvalidCreditDiagnosisRequestException("studentId는 양수여야 합니다.");
        }
        validateAccess(studentId, currentUser);

        GraduationCreditDiagnosisQueryResult queryResult = graduationCreditQueryRepository
                .findCreditDiagnosisByStudentId(studentId)
                .orElseThrow(GraduationCreditDataNotFoundException::new);

        int shortageMajorCredits = shortage(
                queryResult.requiredMajorCredits(),
                queryResult.earnedMajorCredits()
        );
        int shortageGeneralCredits = shortage(
                queryResult.requiredGeneralCredits(),
                queryResult.earnedGeneralCredits()
        );
        int shortageTotalCredits = shortage(
                queryResult.requiredTotalCredits(),
                queryResult.earnedTotalCredits()
        );

        GraduationCreditDiagnosisResult diagnosisResult = new GraduationCreditDiagnosisResult(
                studentId,
                queryResult.earnedMajorCredits(),
                queryResult.earnedGeneralCredits(),
                queryResult.earnedRequiredCredits(),
                queryResult.earnedElectiveCredits(),
                queryResult.earnedTotalCredits(),
                shortageMajorCredits,
                shortageGeneralCredits,
                shortageTotalCredits,
                shortageMajorCredits == 0 && shortageGeneralCredits == 0 && shortageTotalCredits == 0
        );
        return CreditDiagnosisResponseDTO.from(diagnosisResult);
    }

    private void validateAccess(Long studentId, CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new AccessDeniedException("인증 사용자 정보가 없습니다.");
        }

        boolean accessible = switch (currentUser.role()) {
            case "ADMIN" -> true;
            case "STUDENT" -> graduationCreditQueryRepository.isStudentOwnedByUser(studentId, currentUser.id());
            case "PROFESSOR" -> graduationCreditQueryRepository.isStudentAdvisedByUser(studentId, currentUser.id());
            default -> false;
        };
        if (!accessible) {
            throw new AccessDeniedException("해당 학생의 졸업 학점 진단 권한이 없습니다.");
        }
    }

    private int shortage(int requiredCredits, int earnedCredits) {
        return Math.max(requiredCredits - earnedCredits, 0);
    }
}
