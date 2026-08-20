package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LectureSyllabusAccessDeniedException extends BusinessException {

    public LectureSyllabusAccessDeniedException(String message) {
        super(CustomResponseCode.ACCESS_DENIED, message);
    }
}
