package com.msa4lmsv2academic.global.file;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class FileSizeExceededException extends BusinessException {

    public FileSizeExceededException(String message) {
        super(CustomResponseCode.FILE_SIZE_EXCEEDED, message);
    }
}
