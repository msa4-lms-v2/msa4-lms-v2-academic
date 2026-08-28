package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.time.LocalDateTime;
import java.util.List;

public record EnrollmentCartItemQueryResult(
        Long cartItemId,
        LocalDateTime createdAt,
        Long lectureId,
        Long courseId,
        String courseCode,
        String courseName,
        Byte credits,
        Byte targetGrade,
        CompletionType completionType,
        String departmentName,
        String professorName,
        Long semesterId,
        Short academicYear,
        SemesterTerm term,
        String sectionNo,
        String classroom,
        Integer capacity,
        LectureStatus lectureStatus,
        List<EnrollmentCartScheduleQueryResult> schedules
) {
}
