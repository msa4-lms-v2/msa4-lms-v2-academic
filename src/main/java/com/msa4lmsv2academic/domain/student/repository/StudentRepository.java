package com.msa4lmsv2academic.domain.student.repository;

import com.msa4lmsv2academic.domain.student.entity.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"user", "advisor", "advisor.user"})
    Optional<Student> findByUserId(Long userId);

}
