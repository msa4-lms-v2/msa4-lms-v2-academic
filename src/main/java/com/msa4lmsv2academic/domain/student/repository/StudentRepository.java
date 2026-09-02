package com.msa4lmsv2academic.domain.student.repository;

import com.msa4lmsv2academic.domain.student.entity.Student;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"user", "advisor", "advisor.user"})
    Optional<Student> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s join fetch s.user left join fetch s.department "
            + "left join fetch s.doubleMajor left join fetch s.advisor where s.user.id = :userId")
    Optional<Student> findByUserIdForUpdate(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Student s join fetch s.user left join fetch s.department "
            + "left join fetch s.doubleMajor left join fetch s.advisor where s.id = :studentId")
    Optional<Student> findByIdForUpdate(Long studentId);

}
