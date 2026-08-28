package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class CounselingNotificationNotFoundException extends BusinessException {

    public CounselingNotificationNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "상담 알림을 찾을 수 없습니다.");
    }
}
