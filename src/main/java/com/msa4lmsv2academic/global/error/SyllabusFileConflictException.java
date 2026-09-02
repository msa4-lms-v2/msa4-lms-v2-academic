package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class SyllabusFileConflictException extends BusinessException {

    public SyllabusFileConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
