package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class SemesterAccessDeniedException extends BusinessException {

    public SemesterAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학기는 관리자만 등록할 수 있습니다.");
    }
}
