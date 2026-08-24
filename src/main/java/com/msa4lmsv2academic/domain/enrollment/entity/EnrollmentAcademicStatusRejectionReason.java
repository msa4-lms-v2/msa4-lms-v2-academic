package com.msa4lmsv2academic.domain.enrollment.entity;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentAcademicStatusRejectionReason {

    STUDENT_ON_LEAVE(AcademicStatus.ON_LEAVE, "휴학 상태에서는 수강신청할 수 없습니다."),
    STUDENT_WITHDRAWN(AcademicStatus.WITHDRAWN, "자퇴 상태에서는 수강신청할 수 없습니다."),
    STUDENT_GRADUATED(AcademicStatus.GRADUATED, "졸업 상태에서는 수강신청할 수 없습니다."),
    STUDENT_DISMISSED(AcademicStatus.DISMISSED, "제적 상태에서는 수강신청할 수 없습니다.");

    private final AcademicStatus academicStatus;
    private final String message;

    public static EnrollmentAcademicStatusRejectionReason from(AcademicStatus academicStatus) {
        return switch (academicStatus) {
            case ON_LEAVE -> STUDENT_ON_LEAVE;
            case WITHDRAWN -> STUDENT_WITHDRAWN;
            case GRADUATED -> STUDENT_GRADUATED;
            case DISMISSED -> STUDENT_DISMISSED;
            case ENROLLED -> throw new IllegalArgumentException("재학 상태는 수강 가능한 학적 상태입니다.");
        };
    }
}
