package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidProfessorRequestException extends BusinessException {

    public InvalidProfessorRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
