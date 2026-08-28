package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;

public record StudentTimetableScheduleQueryResult(
        Long lectureId,
        LectureDayOfWeek dayOfWeek,
        Byte startPeriod,
        Byte endPeriod
) {
}
