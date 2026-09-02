package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class CounselingScheduleConflictException extends BusinessException {

    public CounselingScheduleConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
