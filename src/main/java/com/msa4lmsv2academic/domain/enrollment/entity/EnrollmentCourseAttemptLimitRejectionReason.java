package com.msa4lmsv2academic.domain.enrollment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentCourseAttemptLimitRejectionReason {
    COURSE_ATTEMPT_LIMIT_EXCEEDED("같은 교과목은 최초 수강과 재수강을 포함해 최대 2회까지 신청할 수 있습니다.");

    private final String message;
}
