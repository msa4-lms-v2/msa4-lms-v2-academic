package com.msa4lmsv2academic.domain.dismissal.repository;

import com.msa4lmsv2academic.domain.dismissal.entity.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface DismissalCandidateRepository extends JpaRepository<DismissalCandidate, Long> {
    boolean existsByStudentIdAndStatus(Long studentId, DismissalStatus status);
    @Query("select d.student.id from DismissalCandidate d where d.id = :id")
    Optional<Long> findStudentIdById(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DismissalCandidate d where d.id = :id")
    Optional<DismissalCandidate> findByIdForUpdate(Long id);
}
