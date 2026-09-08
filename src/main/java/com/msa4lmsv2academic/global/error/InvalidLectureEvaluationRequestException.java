package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidLectureEvaluationRequestException extends BusinessException {

    public InvalidLectureEvaluationRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
