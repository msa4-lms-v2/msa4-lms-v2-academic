package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsBySessionIdAndEnrollmentId(
            Long sessionId,
            Long enrollmentId
    );

    Optional<Attendance> findBySessionIdAndEnrollmentId(
            Long sessionId,
            Long enrollmentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attendance
            from Attendance attendance
            where attendance.session.id = :sessionId
              and attendance.enrollment.id = :enrollmentId
            """)
    Optional<Attendance> findBySessionIdAndEnrollmentIdForUpdate(
            @Param("sessionId") Long sessionId,
            @Param("enrollmentId") Long enrollmentId
    );

    List<Attendance> findAllBySessionId(Long sessionId);

    // 특정 출석 세션에서 지정한 상태에 해당하는 출석 기록 수를 조회
    long countBySessionIdAndStatusIn(
            Long sessionId,
            Collection<AttendanceStatus> statuses
    );


}
