package com.msa4lmsv2academic.domain.infochange.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.InfoChangeRequestStateConflictException;
import org.springframework.stereotype.Component;

@Component
public class StudentInfoChangePolicy {

    public void requireRequestAllowed(AcademicStatus status) {
        if (status != AcademicStatus.ENROLLED && status != AcademicStatus.ON_LEAVE) {
            throw new InfoChangeRequestStateConflictException(
                    "재학 또는 휴학 상태에서만 학생 정보 변경을 신청할 수 있습니다."
            );
        }
    }
}
