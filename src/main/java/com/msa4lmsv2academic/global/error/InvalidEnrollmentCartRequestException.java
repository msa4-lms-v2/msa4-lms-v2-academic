package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidEnrollmentCartRequestException extends BusinessException {

    public InvalidEnrollmentCartRequestException() {
        super(CustomResponseCode.INVALID_PARAMETER, "수강 장바구니 요청 값이 올바르지 않습니다.");
    }
}
