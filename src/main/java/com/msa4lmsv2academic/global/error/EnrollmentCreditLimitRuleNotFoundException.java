package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCreditLimitRuleNotFoundException extends BusinessException {

    public EnrollmentCreditLimitRuleNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "최대 신청학점 규칙을 찾을 수 없습니다.");
    }
}
