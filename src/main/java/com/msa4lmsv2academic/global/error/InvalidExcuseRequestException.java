package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidExcuseRequestException extends BusinessException {

    public InvalidExcuseRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
