package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidEnrollmentCancellationRequestException extends BusinessException {

    public InvalidEnrollmentCancellationRequestException() {
        super(CustomResponseCode.INVALID_PARAMETER, "수강 취소 요청 값이 올바르지 않습니다.");
    }
}
