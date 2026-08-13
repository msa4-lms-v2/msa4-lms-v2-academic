package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NoticeAccessDeniedException extends BusinessException {

    public NoticeAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "해당 공지사항에 접근할 권한이 없습니다.");
    }
}
