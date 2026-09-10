package com.msa4lmsv2academic.domain.coursecorrection.repository;

import com.msa4lmsv2academic.domain.coursecorrection.entity.CourseCorrectionPeriod;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CourseCorrectionPeriodRepository extends JpaRepository<CourseCorrectionPeriod, Long> {

    Optional<CourseCorrectionPeriod> findBySemesterId(Long semesterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select period from CourseCorrectionPeriod period where period.semester.id = :semesterId")
    Optional<CourseCorrectionPeriod> findBySemesterIdForUpdate(Long semesterId);
}
