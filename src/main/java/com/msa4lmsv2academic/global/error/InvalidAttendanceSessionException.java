package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidAttendanceSessionException extends BusinessException {

    public InvalidAttendanceSessionException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
