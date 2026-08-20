package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InfoChangeRequestStateConflictException extends BusinessException {

    public InfoChangeRequestStateConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
