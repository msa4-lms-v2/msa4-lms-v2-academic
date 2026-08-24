package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentAcademicStatusRejectionReason;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import lombok.Getter;

@Getter
public class EnrollmentAcademicStatusNotAllowedException extends BusinessException {

    private final AcademicStatus currentStatus;
    private final EnrollmentAcademicStatusRejectionReason reason;

    public EnrollmentAcademicStatusNotAllowedException(AcademicStatus currentStatus) {
        this(currentStatus, EnrollmentAcademicStatusRejectionReason.from(currentStatus));
    }

    private EnrollmentAcademicStatusNotAllowedException(
            AcademicStatus currentStatus,
            EnrollmentAcademicStatusRejectionReason reason
    ) {
        super(CustomResponseCode.DUPLICATE_DATA, reason.getMessage());
        this.currentStatus = currentStatus;
        this.reason = reason;
    }
}
