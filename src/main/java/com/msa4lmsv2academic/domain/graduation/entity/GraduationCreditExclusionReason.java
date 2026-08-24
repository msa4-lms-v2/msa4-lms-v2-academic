package com.msa4lmsv2academic.domain.graduation.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GraduationCreditExclusionReason {

    ENROLLMENT_CANCELLED("취소된 수강은 졸업학점에 반영되지 않습니다."),
    GRADE_NOT_OPENED("아직 공개되지 않은 성적입니다."),
    GRADE_NOT_ENTERED("공개 상태이지만 성적값이 입력되지 않았습니다."),
    FAILED_GRADE("F 성적은 졸업학점에 반영되지 않습니다."),
    INVALID_GRADE_DATA("허용되지 않은 성적값이므로 졸업학점에서 제외됩니다."),
    RETAKE_DUPLICATE("더 최근의 공개된 합격 재수강 기록이 반영되었습니다.");

    private final String message;
}
