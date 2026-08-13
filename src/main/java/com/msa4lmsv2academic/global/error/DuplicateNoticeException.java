package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateNoticeException extends BusinessException {

    public DuplicateNoticeException() {
        super(CustomResponseCode.DUPLICATE_DATA, "제목, 본문, 대상 역할이 모두 같은 활성 공지사항이 이미 존재합니다.");
    }
}
