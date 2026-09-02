package com.msa4lmsv2academic.domain.withdrawal.repository;

import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicStatusHistoryRepository extends JpaRepository<AcademicStatusHistory, Long> {
}
