package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;

public record CreditDiagnosisSearchCondition(
        long offset,
        int limit,
        String keyword,
        Long departmentId,
        Short admissionYear,
        AcademicStatus academicStatus,
        String sortBy,
        boolean descending,
        Long studentUserId,
        ProfessorStudentScope professorScope
) {
}
