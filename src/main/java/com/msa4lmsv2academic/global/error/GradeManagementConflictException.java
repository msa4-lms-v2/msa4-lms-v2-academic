package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class GradeManagementConflictException extends BusinessException {
    public GradeManagementConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
