package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class LectureEvaluationEnrollmentNotFoundException extends BusinessException {

    public LectureEvaluationEnrollmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "평가할 수 있는 본인의 수강 내역을 찾을 수 없습니다.");
    }
}
