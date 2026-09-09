package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateAdmissionCandidateException extends BusinessException {

    public DuplicateAdmissionCandidateException() {
        super(CustomResponseCode.DUPLICATE_DATA, "이미 등록된 입학 예정자 정보입니다.");
    }

    public DuplicateAdmissionCandidateException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
