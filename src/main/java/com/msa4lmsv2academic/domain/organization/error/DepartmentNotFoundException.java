package com.msa4lmsv2academic.domain.organization.error;

import com.msa4lmsv2academic.global.error.BusinessException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DepartmentNotFoundException extends BusinessException {

    public DepartmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "학과를 찾을 수 없습니다.");
    }
}
