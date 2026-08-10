package com.msa4lmsv2academic.domain.organization.error;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicateDepartmentException extends BusinessException {

    public DuplicateDepartmentException(String message) {
        super(CustomResponseCode.DUPLICATE_DATA, message);
    }
}
