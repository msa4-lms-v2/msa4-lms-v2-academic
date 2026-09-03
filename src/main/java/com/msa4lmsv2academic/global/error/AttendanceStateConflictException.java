package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AttendanceStateConflictException extends BusinessException {

    public AttendanceStateConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
