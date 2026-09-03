package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.AttendanceStateConflictException;
import org.springframework.stereotype.Component;

@Component
public class AttendancePolicy {

    public void requireCheckInAllowed(AcademicStatus status) {
        requireEnrolled(status, "재학 상태인 학생만 출석할 수 있습니다.");
    }

    public void requireExcuseRequestAllowed(AcademicStatus status) {
        requireEnrolled(status, "재학 상태인 학생만 공결을 신청할 수 있습니다.");
    }

    private void requireEnrolled(AcademicStatus status, String message) {
        if (status != AcademicStatus.ENROLLED) {
            throw new AttendanceStateConflictException(message);
        }
    }
}
