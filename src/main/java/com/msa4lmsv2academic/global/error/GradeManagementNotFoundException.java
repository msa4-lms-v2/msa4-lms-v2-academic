package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class GradeManagementNotFoundException extends BusinessException {
    public GradeManagementNotFoundException(String message) {
        super(CustomResponseCode.NOT_FOUND_DATA, message);
    }
}
