package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LectureSyllabusConflictException extends BusinessException {

    public LectureSyllabusConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
