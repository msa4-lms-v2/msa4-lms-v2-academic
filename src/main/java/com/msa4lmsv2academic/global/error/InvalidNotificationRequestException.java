package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidNotificationRequestException extends BusinessException {
    public InvalidNotificationRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
