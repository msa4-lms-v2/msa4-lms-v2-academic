package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselingRecordRepository extends JpaRepository<CounselingRecord, Long> {

    boolean existsByStudentIdAndProfessorIdAndCounselingMethodAndStatus(
            Long studentId,
            Long professorId,
            CounselingMethod counselingMethod,
            CounselingStatus status
    );
}
