package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class SemesterNotFoundException extends BusinessException {

    public SemesterNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학기를 찾을 수 없습니다.");
    }
}
