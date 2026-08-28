package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import java.util.List;

public record StudentTimetableEntryQueryResult(
        Long enrollmentId,
        Long lectureId,
        Long courseId,
        String courseCode,
        String courseName,
        Byte credits,
        CompletionType completionType,
        String professorName,
        String sectionNo,
        String classroom,
        List<StudentTimetableScheduleQueryResult> schedules
) {
}
