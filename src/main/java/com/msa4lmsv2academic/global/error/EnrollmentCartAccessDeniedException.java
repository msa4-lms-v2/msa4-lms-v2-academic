package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCartAccessDeniedException extends BusinessException {

    public EnrollmentCartAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생 본인의 수강 장바구니만 이용할 수 있습니다.");
    }
}
