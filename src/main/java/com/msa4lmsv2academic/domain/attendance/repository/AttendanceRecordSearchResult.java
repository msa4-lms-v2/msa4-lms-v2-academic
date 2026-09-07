package com.msa4lmsv2academic.domain.attendance.repository;

import java.util.List;

public record AttendanceRecordSearchResult(
        List<AttendanceRecordQueryResult> items,
        long totalCount
) {
}
