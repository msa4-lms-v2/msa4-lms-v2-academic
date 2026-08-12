package com.msa4lmsv2academic.domain.professor.repository;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}
