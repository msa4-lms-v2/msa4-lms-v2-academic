package com.msa4lmsv2academic.domain.enrollment.service;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeReasonResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentPrerequisiteRetakeNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 신규 수강신청 저장 전에 최대학점·선수과목·재수강 조건만 검증한다.
 * 호출 서비스는 인증 사용자를 해석한 students.id와 DB에서 조회한 대상 강의를 전달해야 한다.
 * 같은 쓰기 트랜잭션에서 학생 단위 직렬화 등 필요한 잠금을 먼저 획득하고, 검증 후 신청을 저장해야 한다.
 * 학적·신청 기간·중복·정원·시간표·멱등성 검사는 호출 서비스의 책임이며 이 컴포넌트는 저장하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentCourseRuleValidator {

    private final EnrollmentCreditLimitRuleRepository enrollmentCreditLimitRuleRepository;
    private final EnrollmentCreditQueryRepository enrollmentCreditQueryRepository;
    private final PrerequisiteRetakeEvaluator prerequisiteRetakeEvaluator;

    public void validate(Long studentId, Lecture lecture) {
        Long semesterId = lecture.getSemester().getId();
        EnrollmentCreditLimitRule rule = enrollmentCreditLimitRuleRepository
                .findBySemesterIdAndActiveTrue(semesterId)
                .orElseThrow(() -> new EnrollmentCreditLimitNotAllowedException(
                        EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_RULE_NOT_CONFIGURED
                ));
        long activeCredits = enrollmentCreditQueryRepository.sumActiveCredits(studentId, semesterId);
        long requestedTotalCredits = activeCredits + lecture.getCourse().getCredits();
        if (requestedTotalCredits > rule.getMaxCredits()) {
            throw new EnrollmentCreditLimitNotAllowedException(
                    EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_EXCEEDED
            );
        }

        PrerequisiteRetakeEvaluationResponseDTO evaluation = prerequisiteRetakeEvaluator
                .evaluate(studentId, lecture.getCourse());
        if (!evaluation.ruleSatisfied()) {
            throw new EnrollmentPrerequisiteRetakeNotAllowedException(evaluation.reasons().stream()
                    .map(PrerequisiteRetakeReasonResponseDTO::code)
                    .toList());
        }
    }
}
