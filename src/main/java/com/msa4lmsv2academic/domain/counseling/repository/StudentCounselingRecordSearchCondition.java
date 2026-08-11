package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;

public record StudentCounselingRecordSearchCondition(
        long offset,
        int size,
        Long studentUserId,
        CounselingMethod counselingMethod,
        CounselingStatus status
) {
}
