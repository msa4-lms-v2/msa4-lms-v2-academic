package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;

public record CourseGradeAttemptQueryResult(
        Long courseId,
        Long enrollmentId,
        EnrollmentStatus enrollmentStatus,
        GradeStatus gradeStatus,
        String letterGrade,
        short academicYear,
        SemesterTerm term,
        boolean currentSemester
) {
}
