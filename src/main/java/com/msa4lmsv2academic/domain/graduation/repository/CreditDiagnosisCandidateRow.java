package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;

public record CreditDiagnosisCandidateRow(
        Long studentId,
        String studentName,
        Long departmentId,
        String departmentName,
        Short admissionYear,
        AcademicStatus academicStatus,
        Long requirementId,
        Integer requiredMajorCredits,
        Integer requiredGeneralCredits,
        Integer requiredTotalCredits
) {
}
