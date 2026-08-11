package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;

import java.util.List;

public record CounselingRecordSearchResult(
        List<CounselingRecord> items,
        long totalCount
) {
}
