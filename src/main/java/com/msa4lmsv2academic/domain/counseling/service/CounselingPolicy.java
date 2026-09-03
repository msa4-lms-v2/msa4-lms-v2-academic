package com.msa4lmsv2academic.domain.counseling.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.CounselingStatusConflictException;
import org.springframework.stereotype.Component;

@Component
public class CounselingPolicy {

    public void requireAppointmentAllowed(AcademicStatus status) {
        if (status != AcademicStatus.ENROLLED && status != AcademicStatus.ON_LEAVE) {
            throw new CounselingStatusConflictException(
                    "재학 또는 휴학 상태에서만 상담을 신청할 수 있습니다."
            );
        }
    }
}
