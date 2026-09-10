package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NotificationAccessDeniedException extends BusinessException {
    public NotificationAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "알림에 접근할 권한이 없습니다.");
    }
}
