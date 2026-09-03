package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ExcuseReviewConflictException extends BusinessException {

    public ExcuseReviewConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
