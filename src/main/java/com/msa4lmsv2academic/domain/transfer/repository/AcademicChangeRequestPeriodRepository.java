package com.msa4lmsv2academic.domain.transfer.repository;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestPeriod;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AcademicChangeRequestPeriodRepository extends JpaRepository<AcademicChangeRequestPeriod, Long> {
    boolean existsBySemesterIdAndRequestType(Long semesterId, AcademicChangeRequestType requestType);
    Optional<AcademicChangeRequestPeriod> findBySemesterIdAndRequestType(Long semesterId,
                                                                          AcademicChangeRequestType requestType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AcademicChangeRequestPeriod p join fetch p.semester where p.id = :id")
    Optional<AcademicChangeRequestPeriod> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AcademicChangeRequestPeriod p join fetch p.semester "
            + "where p.semester.id = :semesterId and p.requestType = :type")
    Optional<AcademicChangeRequestPeriod> findBySemesterAndTypeForUpdate(Long semesterId,
                                                                         AcademicChangeRequestType type);
}
