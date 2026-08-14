package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidWithdrawalRequestException extends BusinessException {
    public InvalidWithdrawalRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
