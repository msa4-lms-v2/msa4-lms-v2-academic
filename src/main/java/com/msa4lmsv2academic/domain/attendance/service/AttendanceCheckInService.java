package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRepository;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceSessionRepository;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceCheckInRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.AttendanceCheckInResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentRepository;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceCheckInService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceQrService attendanceQrService;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendancePolicy attendancePolicy;

    @Transactional(rollbackFor = Exception.class)
    public AttendanceCheckInResponseDTO checkIn(
            AttendanceCheckInRequestDTO request,
            CurrentUser currentUser
    ) {
        // 출석 세션이 현재 OPEN인지 확인
        AttendanceSession session = attendanceSessionRepository
                .findByIdAndStatus(
                        request.sessionId(),
                        AttendanceSessionStatus.OPEN
                )
                .orElseThrow(() -> new IllegalStateException("진행 중인 출석 세션이 아닙니다."));

        // QR 토큰이 Redis에서 아직 유효한지 확인
        boolean validQr = attendanceQrService.isValid(
                request.sessionId(),
                request.token()
        );

        if(!validQr) {
            throw new IllegalStateException("만료되었거나 유효하지 않은 QR 코드입니다.");
        }

        // 로그인 학생이 해당 강의를 수강중인지 확인
        Enrollment enrollment = enrollmentRepository.findByStudent_User_IdAndLecture_IdAndStatus(
                currentUser.id(),
                session.getLecture().getId(),
                EnrollmentStatus.ACTIVE
        )
                .orElseThrow(() -> new IllegalStateException("해당 강의의 수강생이 아닙니다."));

        attendancePolicy.requireCheckInAllowed(enrollment.getStudent().getAcademicStatus());


        // 이미 출석했는지 확인
        boolean alreadyCheckedIn = attendanceRepository.existsBySessionIdAndEnrollmentId(
                session.getId(),
                enrollment.getId()
        );

        if(alreadyCheckedIn) {
            throw new IllegalStateException(
                    "이미 출석 처리되었습니다."
            );
        }

        // 학생 출석 결과 생성
        LocalDateTime checkInTime = LocalDateTime.now();

        Attendance attendance = Attendance.checkIn(
                enrollment,
                session,
                checkInTime
        );

        // attendances 테이블에 저장
        Attendance saved = attendanceRepository.save(attendance);

        // 체크인 결과 반환
        return new AttendanceCheckInResponseDTO(
                saved.getId(),
                session.getId(),
                session.getLecture().getId(),
                session.getLecture().getCourse().getName(),
                saved.getStatus(),
                saved.getCheckInTime()
        );
    }

}
