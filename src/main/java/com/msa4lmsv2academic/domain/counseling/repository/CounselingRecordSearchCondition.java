package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;

public record CounselingRecordSearchCondition(
        long offset,
        int size,
        Long professorUserId,
        Long studentId,
        CounselingMethod counselingMethod,
        CounselingStatus status
) {
}
