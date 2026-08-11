package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateSemesterException extends BusinessException {

    public DuplicateSemesterException() {
        super(CustomResponseCode.DUPLICATE_DATA, "이미 등록된 학년도와 학기입니다.");
    }
}
