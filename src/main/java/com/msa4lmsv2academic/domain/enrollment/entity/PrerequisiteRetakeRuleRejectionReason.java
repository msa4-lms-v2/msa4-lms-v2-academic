package com.msa4lmsv2academic.domain.enrollment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrerequisiteRetakeRuleRejectionReason {
    PREREQUISITE_NOT_COMPLETED("선수과목을 이수하지 않았습니다."),
    ACTIVE_ENROLLMENT_EXISTS("현재 동일 교과목을 수강 중입니다."),
    RETAKE_BLOCKED_HIGH_GRADE("B 이상 성적 이력이 있어 재수강할 수 없습니다."),
    GRADE_NOT_OPENED("아직 공개되지 않은 성적이 있습니다."),
    GRADE_NOT_ENTERED("공개 상태이지만 성적이 입력되지 않았습니다."),
    INVALID_GRADE_DATA("재수강 판정에 사용할 수 없는 성적 데이터입니다.");

    private final String message;
}
