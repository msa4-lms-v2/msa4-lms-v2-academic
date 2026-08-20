package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidStudentSearchRequestException extends BusinessException {

    public InvalidStudentSearchRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
