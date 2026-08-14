package com.msa4lmsv2academic.domain.academicschedule.repository;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import java.time.LocalDate;
import java.util.Set;

public record AcademicScheduleSearchCondition(
        long offset,
        int limit,
        String keyword,
        LocalDate from,
        LocalDate to,
        Set<AcademicScheduleTargetRole> targetRoles,
        Boolean active
) {
}
