package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCreditLimitRuleStateConflictException extends BusinessException {

    public EnrollmentCreditLimitRuleStateConflictException() {
        super(CustomResponseCode.DUPLICATE_DATA, "수강신청이 시작된 학기의 최대 신청학점 규칙은 변경할 수 없습니다.");
    }
}
