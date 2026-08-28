package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCartConflictException extends BusinessException {

    public EnrollmentCartConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
