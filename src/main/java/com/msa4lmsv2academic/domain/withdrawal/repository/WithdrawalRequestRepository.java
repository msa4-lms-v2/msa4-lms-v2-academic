package com.msa4lmsv2academic.domain.withdrawal.repository;

import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<WithdrawalStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WithdrawalRequest r where r.student.id = :studentId and r.status in :statuses order by r.id")
    java.util.List<WithdrawalRequest> findActiveForUpdate(Long studentId, Collection<WithdrawalStatus> statuses);

    @Query("SELECT request.student.id FROM WithdrawalRequest request WHERE request.id = :id")
    Optional<Long> findStudentIdById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"student", "student.user", "student.advisor", "student.advisor.user",
            "requestedBy", "advisorReviewedBy", "processedBy", "cancelledBy"})
    Page<WithdrawalRequest> findByStudentUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.advisor", "student.advisor.user",
            "requestedBy", "advisorReviewedBy", "processedBy", "cancelledBy"})
    Page<WithdrawalRequest> findByStudentAdvisorUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.advisor", "student.advisor.user",
            "requestedBy", "advisorReviewedBy", "processedBy", "cancelledBy"})
    Page<WithdrawalRequest> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM WithdrawalRequest request WHERE request.id = :id")
    Optional<WithdrawalRequest> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"student", "student.user", "student.advisor", "student.advisor.user",
            "requestedBy", "advisorReviewedBy", "processedBy", "cancelledBy"})
    Optional<WithdrawalRequest> findDetailById(Long id);
}
