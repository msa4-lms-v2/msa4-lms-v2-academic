package com.msa4lmsv2academic.global.error;

import com.msa4lmsv2academic.global.response.CustomResponseCode;

public class PrerequisiteRetakeRuleNotFoundException extends BusinessException {

    public PrerequisiteRetakeRuleNotFoundException() {
        super(CustomResponseCode.NOT_FOUND_DATA, "선수과목 기준정보를 찾을 수 없습니다.");
    }
}
