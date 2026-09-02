package com.msa4lmsv2academic.domain.transfer.repository;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestPeriod;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
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

    @Query("select p from AcademicChangeRequestPeriod p join fetch p.semester "
            + "where p.requestType = :type and p.active = true and p.startAt <= :now and p.endAt >= :now "
            + "order by p.id")
    List<AcademicChangeRequestPeriod> findAccepting(AcademicChangeRequestType type, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AcademicChangeRequestPeriod p join fetch p.semester "
            + "where p.requestType = :type and p.active = true and p.startAt <= :now and p.endAt >= :now "
            + "order by p.id")
    List<AcademicChangeRequestPeriod> findAcceptingForUpdate(AcademicChangeRequestType type, LocalDateTime now);

    @Query("select count(p) from AcademicChangeRequestPeriod p where p.requestType = :type and p.active = true "
            + "and p.id <> :excludedId and p.startAt <= :endAt and p.endAt >= :startAt")
    long countActiveOverlaps(AcademicChangeRequestType type, Long excludedId,
                             LocalDateTime startAt, LocalDateTime endAt);

    @Query("select count(p) from AcademicChangeRequestPeriod p where p.requestType = :type and p.active = true "
            + "and p.startAt <= :endAt and p.endAt >= :startAt")
    long countActiveOverlaps(AcademicChangeRequestType type, LocalDateTime startAt, LocalDateTime endAt);
}
