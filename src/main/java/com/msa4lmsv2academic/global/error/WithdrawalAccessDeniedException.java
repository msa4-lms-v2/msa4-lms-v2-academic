package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class WithdrawalAccessDeniedException extends BusinessException {
    public WithdrawalAccessDeniedException(String message) {
        super(CustomResponseCode.ACCESS_DENIED, message);
    }
}
