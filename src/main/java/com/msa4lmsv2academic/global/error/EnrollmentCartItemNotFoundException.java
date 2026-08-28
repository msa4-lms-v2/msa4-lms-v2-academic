package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCartItemNotFoundException extends BusinessException {

    public EnrollmentCartItemNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "본인의 수강 장바구니 항목을 찾을 수 없습니다.");
    }
}
