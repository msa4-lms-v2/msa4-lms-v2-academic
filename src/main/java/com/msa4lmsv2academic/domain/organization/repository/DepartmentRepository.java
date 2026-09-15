package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Department d order by d.id")
    List<Department> findAllForCodeAllocation();
}
