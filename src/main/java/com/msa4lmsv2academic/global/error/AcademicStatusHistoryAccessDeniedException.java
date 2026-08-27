package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AcademicStatusHistoryAccessDeniedException extends BusinessException {
    public AcademicStatusHistoryAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생·교수·관리자만 학적 변경 이력을 조회할 수 있습니다.");
    }
}
