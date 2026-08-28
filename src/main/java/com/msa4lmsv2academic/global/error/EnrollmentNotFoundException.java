package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentNotFoundException extends BusinessException {

    public EnrollmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "본인의 수강신청을 찾을 수 없습니다.");
    }
}
