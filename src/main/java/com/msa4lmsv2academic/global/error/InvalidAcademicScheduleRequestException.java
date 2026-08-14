package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidAcademicScheduleRequestException extends BusinessException {

    public InvalidAcademicScheduleRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
