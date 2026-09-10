package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.RetakeRejectionReason;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import lombok.Getter;

@Getter
public class EnrollmentRetakeNotAllowedException extends BusinessException {

    private final RetakeRejectionReason reason;

    public EnrollmentRetakeNotAllowedException(RetakeRejectionReason reason) {
        super(CustomResponseCode.DUPLICATE_DATA, reason.getMessage());
        this.reason = reason;
    }
}
