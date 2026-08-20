package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidLectureSyllabusRequestException extends BusinessException {

    public InvalidLectureSyllabusRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
