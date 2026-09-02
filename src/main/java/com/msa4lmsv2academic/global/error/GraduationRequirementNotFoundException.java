package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class GraduationRequirementNotFoundException extends BusinessException {

    public GraduationRequirementNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "졸업 학점요건을 찾을 수 없습니다.");
    }
}
