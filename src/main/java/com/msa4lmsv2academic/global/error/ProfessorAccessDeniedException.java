package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ProfessorAccessDeniedException extends BusinessException {

    public ProfessorAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "교수 인사정보 관리 권한이 없습니다.");
    }
}
