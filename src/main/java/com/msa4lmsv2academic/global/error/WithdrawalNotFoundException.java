package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class WithdrawalNotFoundException extends BusinessException {
    public WithdrawalNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "자퇴 신청을 찾을 수 없습니다.");
    }
}
