package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AcademicScheduleAccessDeniedException extends BusinessException {

    public AcademicScheduleAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "해당 학사일정에 접근할 권한이 없습니다.");
    }
}
