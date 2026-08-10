package com.msa4lmsv2academic.domain.graduation.error;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidCreditDiagnosisRequestException extends BusinessException {

    public InvalidCreditDiagnosisRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
