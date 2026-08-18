package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LectureOpeningAccessDeniedException extends BusinessException {

    public LectureOpeningAccessDeniedException(String message) {
        super(CustomResponseCode.ACCESS_DENIED, message);
    }
}
