package com.msa4lmsv2academic.domain.attendance.repository;

import java.util.List;

public record ExcuseRequestSearchResult(
        List<ExcuseRequestQueryResult> items,
        long totalCount
) {
}
