package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateExcuseRequestException extends BusinessException {

    public DuplicateExcuseRequestException() {
        super(CustomResponseCode.DUPLICATE_DATA, "같은 수강·수업일·교시에 이미 공결을 신청했습니다.");
    }
}
