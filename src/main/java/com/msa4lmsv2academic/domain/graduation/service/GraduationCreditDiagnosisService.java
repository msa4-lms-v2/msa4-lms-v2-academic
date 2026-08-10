package com.msa4lmsv2academic.domain.graduation.service;

import com.msa4lmsv2academic.domain.graduation.entity.CreditCategory;
import com.msa4lmsv2academic.domain.graduation.error.GraduationCreditDataNotFoundException;
import com.msa4lmsv2academic.domain.graduation.error.InvalidCreditDiagnosisRequestException;
import com.msa4lmsv2academic.domain.graduation.repository.EarnedCreditSummaryData;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditDiagnosisData;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditQueryRepository;
import com.msa4lmsv2academic.domain.graduation.repository.GraduationCreditRequirementData;
import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
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

        GraduationCreditDiagnosisData diagnosisData = graduationCreditQueryRepository
                .findCreditDiagnosisByStudentId(studentId)
                .orElseThrow(GraduationCreditDataNotFoundException::new);
        GraduationCreditRequirementData requirement = diagnosisData.requirement();
        EarnedCreditSummaryData earnedCredits = diagnosisData.earnedCredits();

        int shortageMajorCredits = shortage(
                requirement.requiredMajorCredits(),
                earnedCredits.creditsOf(CreditCategory.MAJOR)
        );
        int shortageGeneralCredits = shortage(
                requirement.requiredGeneralCredits(),
                earnedCredits.creditsOf(CreditCategory.GENERAL)
        );
        int shortageTotalCredits = shortage(
                requirement.requiredTotalCredits(),
                earnedCredits.totalCredits()
        );

        return CreditDiagnosisResponseDTO.from(
                studentId,
                earnedCredits,
                shortageMajorCredits,
                shortageGeneralCredits,
                shortageTotalCredits
        );
    }

    private int shortage(int requiredCredits, int earnedCredits) {
        return Math.max(requiredCredits - earnedCredits, 0);
    }
}
