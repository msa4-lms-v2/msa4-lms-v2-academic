package com.msa4lmsv2academic.domain.graduation.repository;

import java.util.Optional;

public interface GraduationCreditQueryRepository {

    Optional<GraduationCreditDiagnosisQueryResult> findCreditDiagnosisByStudentId(Long studentId);
}
