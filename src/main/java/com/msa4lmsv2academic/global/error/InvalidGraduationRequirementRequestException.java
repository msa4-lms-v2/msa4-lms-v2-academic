package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidGraduationRequirementRequestException extends BusinessException {

    public InvalidGraduationRequirementRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
