package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateGraduationRequirementException extends BusinessException {

    public DuplicateGraduationRequirementException() {
        super(CustomResponseCode.DUPLICATE_DATA, "해당 학과·입학연도의 졸업 학점요건이 이미 존재합니다.");
    }
}
