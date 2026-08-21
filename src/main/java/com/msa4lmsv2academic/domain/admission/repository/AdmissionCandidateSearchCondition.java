package com.msa4lmsv2academic.domain.admission.repository;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidateStatus;

public record AdmissionCandidateSearchCondition(
        long offset,
        long limit,
        String keyword,
        Long departmentId,
        Short admissionYear,
        AdmissionCandidateStatus status,
        String sortBy,
        boolean descending
) {
}
