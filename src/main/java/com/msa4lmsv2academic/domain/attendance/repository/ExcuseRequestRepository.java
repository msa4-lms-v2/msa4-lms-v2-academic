package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcuseRequestRepository extends JpaRepository<ExcuseRequest, Long> {

    boolean existsByEnrollmentIdAndLectureDateAndPeriod(
            Long enrollmentId,
            LocalDate lectureDate,
            byte period
    );
}
