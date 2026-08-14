package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateWithdrawalRequestException extends BusinessException {
    public DuplicateWithdrawalRequestException() {
        super(CustomResponseCode.DUPLICATE_DATA, "진행 중인 자퇴 신청이 이미 있습니다.");
    }
}
