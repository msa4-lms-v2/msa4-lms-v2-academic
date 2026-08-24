package com.msa4lmsv2academic.domain.graduation.repository;

import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationRequirementRepository extends JpaRepository<GraduationRequirement, Long> {

    boolean existsByDepartmentIdAndAdmissionYear(Long departmentId, short admissionYear);

    boolean existsByDepartmentIdAndAdmissionYearAndIdNot(Long departmentId, short admissionYear, Long id);
}
