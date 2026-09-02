package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DismissalConflictException extends BusinessException {
    public DismissalConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
