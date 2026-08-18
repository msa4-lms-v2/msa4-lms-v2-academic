package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidLectureOpeningRequestException extends BusinessException {

    public InvalidLectureOpeningRequestException(String message) {
        super(CustomResponseCode.INVALID_PARAMETER, message);
    }
}
