package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class StudentNotFoundException extends BusinessException {

    public StudentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학생 정보를 찾을 수 없습니다.");
    }
}
