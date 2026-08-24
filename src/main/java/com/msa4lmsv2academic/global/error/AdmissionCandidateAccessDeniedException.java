package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AdmissionCandidateAccessDeniedException extends BusinessException {

    public AdmissionCandidateAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "입학 예정자 관리 권한이 없습니다.");
    }
}
