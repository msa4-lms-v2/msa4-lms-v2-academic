package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ProfessorNotFoundException extends BusinessException {

    public ProfessorNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "교수를 찾을 수 없습니다.");
    }
}
