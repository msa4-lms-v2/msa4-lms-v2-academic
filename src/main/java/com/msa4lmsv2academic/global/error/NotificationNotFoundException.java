package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NotificationNotFoundException extends BusinessException {
    public NotificationNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "알림을 찾을 수 없습니다.");
    }
}
