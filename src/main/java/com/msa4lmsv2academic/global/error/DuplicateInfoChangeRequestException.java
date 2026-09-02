package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateInfoChangeRequestException extends BusinessException {
    public DuplicateInfoChangeRequestException() {
        super(CustomResponseCode.DUPLICATE_DATA, "이미 처리 대기 중인 학적 정보 변경 신청이 있습니다.");
    }
}
