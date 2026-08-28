package com.msa4lmsv2academic.domain.leaverequest.repository;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestPeriod;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface LeavePeriodRepository extends JpaRepository<LeaveRequestPeriod, Long> {
    boolean existsBySemesterIdAndRequestType(Long semesterId, LeaveRequestType type);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LeaveRequestPeriod p where p.id = :id")
    Optional<LeaveRequestPeriod> findByIdForUpdate(Long id);
}
