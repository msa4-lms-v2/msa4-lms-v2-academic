package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class WithdrawalIdempotencyConflictException extends BusinessException {
    public WithdrawalIdempotencyConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}

