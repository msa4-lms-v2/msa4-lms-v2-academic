package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidCounselingRecordException extends BusinessException {

    public InvalidCounselingRecordException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
