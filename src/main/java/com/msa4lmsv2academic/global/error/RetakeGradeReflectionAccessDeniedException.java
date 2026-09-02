package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class RetakeGradeReflectionAccessDeniedException extends BusinessException {
    public RetakeGradeReflectionAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "재수강 성적 반영은 관리자만 수행할 수 있습니다.");
    }
}
