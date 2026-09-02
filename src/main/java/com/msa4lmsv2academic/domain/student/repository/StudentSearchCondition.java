package com.msa4lmsv2academic.domain.student.repository;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;

public record StudentSearchCondition(
        long offset,
        long limit,
        String keyword,
        Long departmentId,
        Byte gradeLevel,
        Short admissionYear,
        AcademicStatus academicStatus,
        String sortBy,
        boolean descending,
        ProfessorStudentScope professorScope
) {
}
