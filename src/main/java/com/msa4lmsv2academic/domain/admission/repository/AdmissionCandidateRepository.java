package com.msa4lmsv2academic.domain.admission.repository;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionCandidateRepository extends JpaRepository<AdmissionCandidate, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from AdmissionCandidate c where c.id = :id")
    java.util.Optional<AdmissionCandidate> findByIdForUpdate(Long id);
}
