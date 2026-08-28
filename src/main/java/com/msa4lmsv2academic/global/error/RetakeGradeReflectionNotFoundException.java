package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class RetakeGradeReflectionNotFoundException extends BusinessException {
    public RetakeGradeReflectionNotFoundException(String message) {
        super(CustomResponseCode.NOT_FOUND_DATA, message);
    }
}
