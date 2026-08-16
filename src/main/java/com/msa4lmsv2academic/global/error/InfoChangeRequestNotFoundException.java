package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InfoChangeRequestNotFoundException extends BusinessException {
    public InfoChangeRequestNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학적 정보 변경 신청을 찾을 수 없습니다.");
    }
}
