package com.msa4lmsv2academic.domain.grade.repository;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.math.BigDecimal;

public record StudentGradeQueryResult(
        Long enrollmentId,
        Long courseId,
        Long classId,
        short academicYear,
        SemesterTerm term,
        String courseCode,
        String courseName,
        byte credits,
        BigDecimal totalScore,
        String letterGrade
) {
}
