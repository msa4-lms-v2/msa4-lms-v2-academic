package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidCounselingRequestException extends BusinessException {

    public InvalidCounselingRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
