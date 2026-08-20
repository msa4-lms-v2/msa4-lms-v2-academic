package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentInfoChangeRequestRepository extends JpaRepository<StudentInfoChangeRequest, Long> {

    boolean existsByStudentIdAndStatus(Long studentId, InfoChangeRequestStatus status);

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "reviewedBy"})
    Page<StudentInfoChangeRequest> findByStudentUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "reviewedBy"})
    Page<StudentInfoChangeRequest> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "reviewedBy"})
    Optional<StudentInfoChangeRequest> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM StudentInfoChangeRequest request WHERE request.id = :id")
    Optional<StudentInfoChangeRequest> findByIdForUpdate(@Param("id") Long id);
}
