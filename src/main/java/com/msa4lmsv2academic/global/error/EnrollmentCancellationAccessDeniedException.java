package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentCancellationAccessDeniedException extends BusinessException {

    public EnrollmentCancellationAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생 본인의 수강신청만 취소할 수 있습니다.");
    }
}
