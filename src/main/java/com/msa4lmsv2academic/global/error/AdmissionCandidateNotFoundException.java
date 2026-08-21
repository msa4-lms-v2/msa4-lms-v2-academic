package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AdmissionCandidateNotFoundException extends BusinessException {

    public AdmissionCandidateNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "입학 예정자를 찾을 수 없습니다.");
    }
}
