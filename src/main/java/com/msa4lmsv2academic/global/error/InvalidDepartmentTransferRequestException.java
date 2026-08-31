package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidDepartmentTransferRequestException extends BusinessException {
    public InvalidDepartmentTransferRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
