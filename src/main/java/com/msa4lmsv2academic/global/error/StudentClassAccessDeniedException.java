package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class StudentClassAccessDeniedException extends BusinessException {

    public StudentClassAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생 본인의 강의만 조회할 수 있습니다.");
    }
}
