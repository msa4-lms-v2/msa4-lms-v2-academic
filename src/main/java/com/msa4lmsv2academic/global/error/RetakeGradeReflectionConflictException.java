package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class RetakeGradeReflectionConflictException extends BusinessException {
    public RetakeGradeReflectionConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
