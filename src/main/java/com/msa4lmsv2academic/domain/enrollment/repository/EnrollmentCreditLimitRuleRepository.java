package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentCreditLimitRuleRepository
        extends JpaRepository<EnrollmentCreditLimitRule, Long> {

    boolean existsBySemesterId(Long semesterId);

    Optional<EnrollmentCreditLimitRule> findBySemesterIdAndActiveTrue(Long semesterId);
}
