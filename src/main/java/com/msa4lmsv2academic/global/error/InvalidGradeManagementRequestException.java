package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidGradeManagementRequestException extends BusinessException {
    public InvalidGradeManagementRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
