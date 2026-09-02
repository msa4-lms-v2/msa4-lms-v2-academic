package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidLeaveRequestException extends BusinessException {
    public InvalidLeaveRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
