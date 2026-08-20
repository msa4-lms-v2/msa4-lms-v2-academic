package com.msa4lmsv2academic.domain.professor.repository;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    @EntityGraph(attributePaths = {"user", "department", "department.college"})
    Optional<Professor> findByUserId(Long userId);
}
