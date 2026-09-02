package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;

public record ProfessorLectureScheduleQueryResult(
        Long classId,
        LectureDayOfWeek dayOfWeek,
        Byte startPeriod,
        Byte endPeriod
) {
}
