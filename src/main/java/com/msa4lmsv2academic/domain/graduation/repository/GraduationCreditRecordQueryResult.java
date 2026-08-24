package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;

public record GraduationCreditRecordQueryResult(
        Long enrollmentId,
        Long courseId,
        String courseCode,
        String courseName,
        Byte credits,
        CompletionType completionType,
        Short academicYear,
        SemesterTerm term,
        EnrollmentStatus enrollmentStatus,
        GradeStatus gradeStatus,
        String letterGrade
) {
}
