package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateProfileEmailException extends BusinessException {

    public DuplicateProfileEmailException() {
        super(CustomResponseCode.DUPLICATE_DATA, "이미 사용 중인 이메일입니다.");
    }
}
