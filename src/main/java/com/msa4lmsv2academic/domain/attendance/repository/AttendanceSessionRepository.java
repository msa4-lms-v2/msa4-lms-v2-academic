package com.msa4lmsv2academic.domain.attendance.repository;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findByIdAndStatus(
            Long id,
            AttendanceSessionStatus status
    );

    Optional<AttendanceSession> findByLectureIdAndSessionDateAndPeriod(
            Long lectureId,
            LocalDate sessionDate,
            Integer period
    );

    // QR 갱신 권한 확인 메서드
    Optional<AttendanceSession>
    findByIdAndStatusAndLecture_Professor_User_Id(
            Long sessionId,
            AttendanceSessionStatus status,
            Long professorUserId
    );

    // 현재 세션 조회
    Optional<AttendanceSession>
    findFirstByLectureIdAndStatusAndLecture_Professor_User_IdOrderByOpenedAtDesc(
            Long classId,
            AttendanceSessionStatus status,
            Long professorUserId
    );

    // 세션 소유 교수 검증
    Optional<AttendanceSession> findByIdAndLecture_Professor_User_Id(
            Long sessionId,
            Long professorUserId
    );

    // 출석 세션 조회
    Page<AttendanceSession>
    findAllByLecture_Professor_User_IdAndLecture_Semester_CurrentTrueOrderByOpenedAtDesc(
            Long professorUserId,
            Pageable pageable
    );
}
