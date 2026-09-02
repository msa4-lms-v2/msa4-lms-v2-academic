package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AcademicScheduleNotFoundException extends BusinessException {

    public AcademicScheduleNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학사일정을 찾을 수 없습니다.");
    }
}
