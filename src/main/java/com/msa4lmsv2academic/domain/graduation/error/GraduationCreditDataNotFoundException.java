package com.msa4lmsv2academic.domain.graduation.error;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class GraduationCreditDataNotFoundException extends BusinessException {

    public GraduationCreditDataNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학생의 학점 또는 졸업요건 정보를 찾을 수 없습니다.");
    }
}
