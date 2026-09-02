package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidSemesterRequestException extends BusinessException {

    public InvalidSemesterRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
