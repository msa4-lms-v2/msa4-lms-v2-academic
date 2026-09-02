package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class CounselingAppointmentNotFoundException extends BusinessException {

    public CounselingAppointmentNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "상담 예약을 찾을 수 없습니다.");
    }
}
