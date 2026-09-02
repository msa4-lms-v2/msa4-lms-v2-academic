package com.msa4lmsv2academic.domain.grade.repository;

import com.msa4lmsv2academic.domain.grade.entity.GradeCorrectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeCorrectionHistoryRepository extends JpaRepository<GradeCorrectionHistory, Long> {

    boolean existsByEnrollmentIdAndFieldChangedAndNewValue(
            Long enrollmentId,
            String fieldChanged,
            String newValue
    );
}
