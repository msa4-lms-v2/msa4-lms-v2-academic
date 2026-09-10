package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCourseAttemptLimitRejectionReason;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import lombok.Getter;

@Getter
public class EnrollmentCourseAttemptLimitNotAllowedException extends BusinessException {

    private final EnrollmentCourseAttemptLimitRejectionReason reason;

    public EnrollmentCourseAttemptLimitNotAllowedException(EnrollmentCourseAttemptLimitRejectionReason reason) {
        super(CustomResponseCode.DUPLICATE_DATA, reason.getMessage());
        this.reason = reason;
    }
}
