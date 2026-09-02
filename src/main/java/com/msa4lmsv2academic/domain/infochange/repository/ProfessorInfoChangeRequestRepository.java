package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessorInfoChangeRequestRepository extends JpaRepository<ProfessorInfoChangeRequest, Long> {

    boolean existsByProfessorIdAndStatus(Long professorId, InfoChangeRequestStatus status);

    @EntityGraph(attributePaths = {"professor", "professor.user", "professor.department", "reviewedBy"})
    Optional<ProfessorInfoChangeRequest> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"professor", "professor.user", "professor.department"})
    @Query("SELECT request FROM ProfessorInfoChangeRequest request WHERE request.id = :id")
    Optional<ProfessorInfoChangeRequest> findByIdForUpdate(@Param("id") Long id);
}
