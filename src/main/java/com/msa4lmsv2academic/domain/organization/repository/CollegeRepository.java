package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
}
