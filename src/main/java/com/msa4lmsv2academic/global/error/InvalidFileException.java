package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class InvalidFileException extends BusinessException {

    public InvalidFileException(String message) {
        super(CustomResponseCode.FILE_ERROR, message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(CustomResponseCode.FILE_ERROR, message, cause);
    }
}
