package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidDismissalRequestException extends BusinessException {
    public InvalidDismissalRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
