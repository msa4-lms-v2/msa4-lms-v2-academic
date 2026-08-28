package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LeaveRequestConflictException extends BusinessException {
    public LeaveRequestConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
