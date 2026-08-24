package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class DuplicatePrerequisiteRetakeRuleException extends BusinessException {

    public DuplicatePrerequisiteRetakeRuleException() {
        super(CustomResponseCode.DUPLICATE_DATA, "동일한 대상·선수 교과목 기준이 이미 존재합니다.");
    }
}
