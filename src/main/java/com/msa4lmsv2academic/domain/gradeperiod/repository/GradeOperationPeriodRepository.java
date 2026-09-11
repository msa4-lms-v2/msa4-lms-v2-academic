package com.msa4lmsv2academic.domain.gradeperiod.repository;

import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationPeriod;
import com.msa4lmsv2academic.domain.gradeperiod.entity.GradeOperationType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GradeOperationPeriodRepository extends JpaRepository<GradeOperationPeriod, Long> {

    Optional<GradeOperationPeriod> findBySemesterIdAndOperationType(
            Long semesterId,
            GradeOperationType operationType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select period from GradeOperationPeriod period "
            + "where period.semester.id = :semesterId and period.operationType = :operationType")
    Optional<GradeOperationPeriod> findBySemesterIdAndOperationTypeForUpdate(
            Long semesterId,
            GradeOperationType operationType
    );
}
