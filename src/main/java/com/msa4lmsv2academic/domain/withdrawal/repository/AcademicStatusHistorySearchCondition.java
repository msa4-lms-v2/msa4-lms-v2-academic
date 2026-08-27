package com.msa4lmsv2academic.domain.withdrawal.repository;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistorySourceType;
import java.time.LocalDate;

public record AcademicStatusHistorySearchCondition(
        String keyword,
        Long studentId,
        Long departmentId,
        AcademicStatus previousStatus,
        AcademicStatus newStatus,
        AcademicStatusHistorySourceType sourceType,
        LocalDate fromDate,
        LocalDate toDate,
        Long ownerUserId,
        ProfessorStudentScope professorScope
) {
}
