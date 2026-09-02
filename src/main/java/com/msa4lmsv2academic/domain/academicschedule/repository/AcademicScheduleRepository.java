package com.msa4lmsv2academic.domain.academicschedule.repository;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, Long> {
}
