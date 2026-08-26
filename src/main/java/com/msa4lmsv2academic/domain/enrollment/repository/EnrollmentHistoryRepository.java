package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentHistoryRepository extends JpaRepository<EnrollmentHistory, Long> {
}
