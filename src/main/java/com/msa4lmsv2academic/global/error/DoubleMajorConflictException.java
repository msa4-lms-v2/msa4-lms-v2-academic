package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DoubleMajorConflictException extends BusinessException {
    public DoubleMajorConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
