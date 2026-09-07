package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class AttendanceRecordNotFoundException extends BusinessException {

    public AttendanceRecordNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "출결 기록을 찾을 수 없습니다.");
    }
}
