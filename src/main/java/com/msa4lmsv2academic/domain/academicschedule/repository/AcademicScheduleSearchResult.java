package com.msa4lmsv2academic.domain.academicschedule.repository;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import java.util.List;

public record AcademicScheduleSearchResult(
        List<AcademicSchedule> items,
        long totalCount
) {
}
