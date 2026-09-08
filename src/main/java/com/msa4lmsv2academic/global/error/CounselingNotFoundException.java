package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class CounselingNotFoundException extends BusinessException {
    public CounselingNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "상담을 찾을 수 없습니다.");
    }
}
