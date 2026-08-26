package com.msa4lmsv2academic.domain.enrollment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EnrollmentApplicationRejectionReason {
    LECTURE_NOT_OPEN("신청 가능한 개설 강의가 아닙니다."),
    ENROLLMENT_PERIOD_CLOSED("수강신청 기간이 아닙니다."),
    DUPLICATE_ENROLLMENT("이미 신청한 강의입니다."),
    CAPACITY_EXCEEDED("수강 정원이 마감되었습니다."),
    SCHEDULE_CONFLICT("이미 신청한 강의와 시간이 겹칩니다."),
    IDEMPOTENCY_KEY_CONFLICT("동일한 멱등 키가 다른 요청에 사용되었거나 처리 중입니다.");

    private final String message;
}
