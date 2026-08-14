package com.msa4lmsv2academic.domain.student.repository;

import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"user", "advisor", "advisor.user"})
    Optional<Student> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT student
            FROM Student student
            JOIN FETCH student.user
            LEFT JOIN FETCH student.advisor advisor
            LEFT JOIN FETCH advisor.user
            WHERE student.user.id = :userId
            """)
    Optional<Student> findByUserIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT student FROM Student student JOIN FETCH student.user WHERE student.id = :studentId")
    Optional<Student> findByIdForUpdate(@Param("studentId") Long studentId);
}
