package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ExcuseAttachmentConflictException extends BusinessException {

    public ExcuseAttachmentConflictException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
