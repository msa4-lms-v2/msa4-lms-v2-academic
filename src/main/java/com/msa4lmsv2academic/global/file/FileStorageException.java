package com.msa4lmsv2academic.global.file;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class FileStorageException extends BusinessException {

    public FileStorageException(String message) {
        super(CustomResponseCode.SYSTEM_ERROR, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(CustomResponseCode.SYSTEM_ERROR, message, cause);
    }
}
