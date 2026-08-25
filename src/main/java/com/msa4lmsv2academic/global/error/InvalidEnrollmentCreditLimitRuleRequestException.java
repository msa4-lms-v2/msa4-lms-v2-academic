package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidEnrollmentCreditLimitRuleRequestException extends BusinessException {

    public InvalidEnrollmentCreditLimitRuleRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
