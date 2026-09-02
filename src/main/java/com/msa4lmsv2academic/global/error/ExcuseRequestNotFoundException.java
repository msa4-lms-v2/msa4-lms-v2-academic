package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ExcuseRequestNotFoundException extends BusinessException {

    public ExcuseRequestNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "공결 신청을 찾을 수 없습니다.");
    }
}
