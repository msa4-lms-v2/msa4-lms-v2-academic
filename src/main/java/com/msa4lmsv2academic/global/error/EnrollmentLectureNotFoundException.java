package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class EnrollmentLectureNotFoundException extends BusinessException {
    public EnrollmentLectureNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "신청 대상 강의를 찾을 수 없습니다.");
    }
}
