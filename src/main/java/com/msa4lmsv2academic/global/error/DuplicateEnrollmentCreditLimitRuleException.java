package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateEnrollmentCreditLimitRuleException extends BusinessException {

    public DuplicateEnrollmentCreditLimitRuleException() {
        super(CustomResponseCode.DUPLICATE_DATA, "해당 학기의 최대 신청학점 규칙이 이미 존재합니다.");
    }
}
