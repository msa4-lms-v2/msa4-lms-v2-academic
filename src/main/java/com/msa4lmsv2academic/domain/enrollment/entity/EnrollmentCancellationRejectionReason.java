package com.msa4lmsv2academic.domain.enrollment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentCancellationRejectionReason {

    ENROLLMENT_PERIOD_CLOSED("수강신청 기간에만 수강을 취소할 수 있습니다."),
    ENROLLMENT_ALREADY_CANCELLED("이미 취소된 수강입니다.");

    private final String message;
}
