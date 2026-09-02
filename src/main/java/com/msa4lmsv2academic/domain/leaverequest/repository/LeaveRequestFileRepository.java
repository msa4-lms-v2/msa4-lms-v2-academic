package com.msa4lmsv2academic.domain.leaverequest.repository;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestFileRepository extends JpaRepository<LeaveRequestFile, Long> {
    Optional<LeaveRequestFile> findByIdAndRequestId(Long id, Long requestId);
}
