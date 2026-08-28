package com.msa4lmsv2academic.domain.leaverequest.repository;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequest;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestStatus;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    boolean existsByStudentIdAndStatus(Long studentId, LeaveRequestStatus status);
    boolean existsByStudentIdAndRequestTypeAndStatus(Long studentId, LeaveRequestType type, LeaveRequestStatus status);

    @Query("select r.student.id from LeaveRequest r where r.id = :id")
    Optional<Long> findStudentIdById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LeaveRequest r where r.id = :id")
    Optional<LeaveRequest> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from LeaveRequest r where r.student.id = :studentId and r.status = 'PENDING' order by r.id")
    List<LeaveRequest> findPendingForUpdate(Long studentId);
}
