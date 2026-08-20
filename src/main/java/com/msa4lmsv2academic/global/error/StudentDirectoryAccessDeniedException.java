package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class StudentDirectoryAccessDeniedException extends BusinessException {

    public StudentDirectoryAccessDeniedException(String message) {
        super(CustomResponseCode.ACCESS_DENIED, message);
    }
}
