package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NoticeAuthorNotFoundException extends BusinessException {

    public NoticeAuthorNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "Academic에 동기화된 관리자 정보를 찾을 수 없습니다.");
    }
}
