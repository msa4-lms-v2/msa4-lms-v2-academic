package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateCounselingRequestException extends BusinessException {

    public DuplicateCounselingRequestException() {
        super(CustomResponseCode.DUPLICATE_DATA, "답변 대기 중인 온라인 상담이 이미 있습니다.");
    }
}
