package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRejectionReason;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import lombok.Getter;

@Getter
public class EnrollmentCreditLimitNotAllowedException extends BusinessException {

    private final EnrollmentCreditLimitRejectionReason reason;

    public EnrollmentCreditLimitNotAllowedException(EnrollmentCreditLimitRejectionReason reason) {
        super(CustomResponseCode.DUPLICATE_DATA, reason.getMessage());
        this.reason = reason;
    }
}
