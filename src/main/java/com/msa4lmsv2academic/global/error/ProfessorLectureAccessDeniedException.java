package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class ProfessorLectureAccessDeniedException extends BusinessException {

    public ProfessorLectureAccessDeniedException() {
        super(CustomResponseCode.ACCESS_DENIED, "교수 본인의 담당 강의만 조회할 수 있습니다.");
    }
}
