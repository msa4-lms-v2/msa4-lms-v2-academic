package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ProfessorDepartmentNotFoundException extends BusinessException {

    public ProfessorDepartmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "변경할 학과를 찾을 수 없습니다.");
    }
}
