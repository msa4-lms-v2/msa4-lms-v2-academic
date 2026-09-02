package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateAcademicScheduleException extends BusinessException {

    public DuplicateAcademicScheduleException() {
        super(CustomResponseCode.DUPLICATE_DATA, "동일한 활성 학사일정이 이미 존재합니다.");
    }
}
