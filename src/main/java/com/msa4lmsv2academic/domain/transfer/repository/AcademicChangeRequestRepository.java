package com.msa4lmsv2academic.domain.transfer.repository;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequest;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestStatus;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AcademicChangeRequestRepository extends JpaRepository<AcademicChangeRequest, Long> {
    boolean existsByStudentIdAndRequestTypeAndStatus(Long studentId, AcademicChangeRequestType requestType,
                                                     AcademicChangeRequestStatus status);

    @Query("select r.student.id from AcademicChangeRequest r where r.id = :id and r.requestType = :type")
    Optional<Long> findStudentIdByIdAndType(Long id, AcademicChangeRequestType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AcademicChangeRequest r where r.id = :id and r.requestType = :type")
    Optional<AcademicChangeRequest> findByIdAndTypeForUpdate(Long id, AcademicChangeRequestType type);
}
