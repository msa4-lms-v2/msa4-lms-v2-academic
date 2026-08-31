package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class WithdrawalAttachmentNotFoundException extends BusinessException {
    public WithdrawalAttachmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "자퇴 증빙 파일을 찾을 수 없습니다.");
    }
}
