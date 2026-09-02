package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCreditLimitRuleAccessDeniedException extends BusinessException {

    public EnrollmentCreditLimitRuleAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "최대 신청학점 규칙은 관리자만 관리할 수 있습니다.");
    }
}
