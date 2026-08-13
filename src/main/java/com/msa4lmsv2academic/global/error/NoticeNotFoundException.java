package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NoticeNotFoundException extends BusinessException {

    public NoticeNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "공지사항을 찾을 수 없습니다.");
    }
}
