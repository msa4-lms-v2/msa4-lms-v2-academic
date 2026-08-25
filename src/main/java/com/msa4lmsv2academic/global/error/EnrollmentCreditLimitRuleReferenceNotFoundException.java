package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCreditLimitRuleReferenceNotFoundException extends BusinessException {

    public EnrollmentCreditLimitRuleReferenceNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "최대 신청학점 규칙에 적용할 학기를 찾을 수 없습니다.");
    }
}
