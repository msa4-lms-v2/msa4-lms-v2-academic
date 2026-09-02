package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ExcuseRequestRepository extends JpaRepository<ExcuseRequest, Long> {

    boolean existsByEnrollmentIdAndLectureDateAndPeriod(
            Long enrollmentId,
            LocalDate lectureDate,
            byte period
    );

    @Query("""
            select excuseRequest
            from ExcuseRequest excuseRequest
            join fetch excuseRequest.enrollment enrollment
            join fetch enrollment.student student
            join fetch student.user studentUser
            join fetch enrollment.lecture lecture
            join fetch lecture.professor professor
            join fetch professor.user professorUser
            where excuseRequest.id = :requestId
            """)
    Optional<ExcuseRequest> findDetailById(@Param("requestId") Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select excuseRequest
            from ExcuseRequest excuseRequest
            join fetch excuseRequest.enrollment enrollment
            join fetch enrollment.student student
            join fetch student.user studentUser
            join fetch enrollment.lecture lecture
            join fetch lecture.professor professor
            join fetch professor.user professorUser
            where excuseRequest.id = :requestId
            """)
    Optional<ExcuseRequest> findDetailForUpdate(@Param("requestId") Long requestId);
}
