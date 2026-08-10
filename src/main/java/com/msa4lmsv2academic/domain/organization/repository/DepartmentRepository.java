package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);

    boolean existsByCollegeIdAndName(Long collegeId, String name);

    boolean existsByCollegeIdAndNameAndIdNot(Long collegeId, String name, Long id);
}
