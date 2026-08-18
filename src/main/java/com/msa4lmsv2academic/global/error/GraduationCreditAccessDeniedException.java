package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class GraduationCreditAccessDeniedException extends BusinessException {

    public GraduationCreditAccessDeniedException(String message) {
        super(CustomResponseCode.ACCESS_DENIED, message);
    }
}
