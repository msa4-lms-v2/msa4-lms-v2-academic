package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidEnrollmentApplicationRequestException extends BusinessException {
    public InvalidEnrollmentApplicationRequestException() {
        super(CustomResponseCode.INVALID_PARAMETER, "양수 lectureId와 1~100자의 공백 없는 Idempotency-Key가 필요합니다.");
    }
}
