package com.msa4lmsv2academic.domain.admission.repository;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionCandidateRepository extends JpaRepository<AdmissionCandidate, Long> {

    boolean existsByApplicationNumber(String applicationNumber);
}
