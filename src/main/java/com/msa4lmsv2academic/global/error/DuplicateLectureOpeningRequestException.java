package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateLectureOpeningRequestException extends BusinessException {

    public DuplicateLectureOpeningRequestException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
