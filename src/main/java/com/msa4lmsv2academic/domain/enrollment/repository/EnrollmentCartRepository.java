package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentCartRepository extends JpaRepository<EnrollmentCart, Long> {
}
