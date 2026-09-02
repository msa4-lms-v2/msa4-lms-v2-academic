package com.msa4lmsv2academic.domain.enrollment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentCreditLimitRejectionReason {

    CREDIT_LIMIT_RULE_NOT_CONFIGURED("해당 학기의 활성 최대 신청학점 규칙이 없습니다."),
    CREDIT_LIMIT_EXCEEDED("해당 학기의 최대 신청학점을 초과합니다.");

    private final String message;
}
