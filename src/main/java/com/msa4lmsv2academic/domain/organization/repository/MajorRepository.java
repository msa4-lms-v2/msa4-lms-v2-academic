package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.College;
import com.msa4lmsv2academic.domain.organization.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {
    @Query("select m from Major m join fetch m.department d left join fetch d.college where m.id = :id")
    Optional<Major> findDetailById(Long id);
}
