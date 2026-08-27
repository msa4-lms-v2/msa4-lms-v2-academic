package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class WithdrawalStateConflictException extends BusinessException {
    public WithdrawalStateConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}

