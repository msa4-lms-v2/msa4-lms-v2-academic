package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class NoticeStateConflictException extends BusinessException {

    public NoticeStateConflictException(boolean active) {
        super(
                CustomResponseCode.DUPLICATE_DATA,
                active ? "이미 활성 상태인 공지사항입니다." : "이미 비활성 상태인 공지사항입니다."
        );
    }
}
