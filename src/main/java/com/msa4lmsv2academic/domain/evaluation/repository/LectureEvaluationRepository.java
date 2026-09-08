package com.msa4lmsv2academic.domain.evaluation.repository;

import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureEvaluationRepository extends JpaRepository<LectureEvaluation, Long> {

    boolean existsByEnrollment_Id(Long enrollmentId);
}
