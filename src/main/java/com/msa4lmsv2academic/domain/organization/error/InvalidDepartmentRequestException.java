package com.msa4lmsv2academic.domain.organization.error;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidDepartmentRequestException extends BusinessException {

    public InvalidDepartmentRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
