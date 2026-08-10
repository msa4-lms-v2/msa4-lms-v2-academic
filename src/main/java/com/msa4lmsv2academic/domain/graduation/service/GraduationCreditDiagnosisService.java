package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisQueryResult;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.global.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.global.error.InvalidCreditDiagnosisRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(GraduationCreditQueryRepository.class)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationCreditDiagnosisService {

    private final GraduationCreditQueryRepository graduationCreditQueryRepository;

    public CreditDiagnosisResponseDTO diagnose(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new InvalidCreditDiagnosisRequestException("studentId는 양수여야 합니다.");
        }

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

    private int shortage(int requiredCredits, int earnedCredits) {
        return Math.max(requiredCredits - earnedCredits, 0);
    }
}
