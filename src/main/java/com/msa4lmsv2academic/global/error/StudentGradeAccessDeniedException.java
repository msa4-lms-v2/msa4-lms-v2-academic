package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class StudentGradeAccessDeniedException extends BusinessException {

    public StudentGradeAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생 본인의 공개 성적만 조회할 수 있습니다.");
    }
}
