package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class EnrollmentPrerequisiteRetakeNotAllowedException extends BusinessException {

    private final List<PrerequisiteRetakeRuleRejectionReason> reasons;

    public EnrollmentPrerequisiteRetakeNotAllowedException(List<PrerequisiteRetakeRuleRejectionReason> reasons) {
        super(CustomResponseCode.DUPLICATE_DATA, reasons.stream()
                .map(PrerequisiteRetakeRuleRejectionReason::getMessage)
                .collect(Collectors.joining(" ")));
        this.reasons = List.copyOf(reasons);
    }
}
