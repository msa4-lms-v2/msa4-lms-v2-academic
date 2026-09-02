package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.time.LocalDateTime;

public record StudentEnrollmentQueryResult(
        Long enrollmentId,
        EnrollmentStatus enrollmentStatus,
        LocalDateTime enrolledAt,
        Long classId,
        Long courseId,
        String courseCode,
        String courseName,
        Byte credits,
        Byte targetGrade,
        CompletionType completionType,
        String departmentName,
        String professorName,
        Short academicYear,
        SemesterTerm term,
        String sectionNo,
        String classroom,
        Integer capacity,
        LectureStatus status
) {
}
