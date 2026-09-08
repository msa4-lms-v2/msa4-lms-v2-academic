package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LectureEvaluationAccessDeniedException extends BusinessException {

    public LectureEvaluationAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "학생 본인의 강의평가만 제출할 수 있습니다.");
    }
}
