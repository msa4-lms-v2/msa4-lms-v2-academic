package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class CounselingRecordNotFoundException extends BusinessException {

    public CounselingRecordNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "상담 기록을 찾을 수 없습니다.");
    }
}
