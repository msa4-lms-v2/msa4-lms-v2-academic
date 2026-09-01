package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSession;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceSessionStatus;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceRepository;
import com.msa4lmsv2academic.domain.attendance.repository.AttendanceSessionRepository;
import com.msa4lmsv2academic.domain.attendance.request.AttendanceSessionCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.*;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentApplicationQueryRepository;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.entity.LectureSchedule;
import com.msa4lmsv2academic.domain.lecture.repository.LectureRepository;
import com.msa4lmsv2academic.domain.lecture.repository.LectureScheduleRepository;
import com.msa4lmsv2academic.global.error.ProfessorLectureAccessDeniedException;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceSessionService {

    private final LectureRepository lectureRepository;
    private final LectureScheduleRepository lectureScheduleRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentApplicationQueryRepository enrollmentQueryRepository;
    private final AttendanceQrService attendanceQrService;
    private final AttendanceRepository attendanceRepository;

    @Transactional(rollbackFor = Exception.class)
    public AttendanceSessionOpenResponseDTO open(
            AttendanceSessionCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 선택한 강의가 현재 학기이면서 로그인 교수의 담당 강의인지 확인
        Lecture lecture = lectureRepository
                .findByIdAndProfessor_User_IdAndSemester_CurrentTrue(
                        request.classId(),
                        currentUser.id()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "현재 학기에 담당하는 강의를 찾을 수 없습니다."
                ));

        // Java 요일을 lecture_schedules에서 사용하는 요일 값으로 변환
        LectureDayOfWeek lectureDay = switch (today.getDayOfWeek()) {
            case MONDAY -> LectureDayOfWeek.MON;
            case TUESDAY -> LectureDayOfWeek.TUE;
            case WEDNESDAY -> LectureDayOfWeek.WED;
            case THURSDAY -> LectureDayOfWeek.THU;
            case FRIDAY -> LectureDayOfWeek.FRI;
            case SATURDAY, SUNDAY -> throw new IllegalStateException(
                    "주말에는 예정된 강의를 찾을 수 없습니다."
            );
        };

        // 해당 강의의 오늘 시간표를 조회
        List<LectureSchedule> schedules = lectureScheduleRepository
                .findAllByLectureIdAndDayOfWeekOrderByStartPeriodAsc(
                        lecture.getId(),
                        lectureDay
                );

        if (schedules.isEmpty()) {
            throw new IllegalStateException("오늘 예정된 강의가 없습니다.");
        }

        if (schedules.size() > 1) {
            throw new IllegalStateException(
                    "오늘 동일 강의의 시간표가 여러 개라 교시를 자동으로 결정할 수 없습니다."
            );
        }

        int period = schedules.getFirst().getStartPeriod();

        // 같은 강의·날짜·교시의 세션이 있으면 종료 여부에 따라 재사용
        AttendanceSession saved = attendanceSessionRepository
                .findByLectureIdAndSessionDateAndPeriod(
                        lecture.getId(),
                        today,
                        period
                )
                .map(existingSession -> {
                    // 종료한 세션이면 같은 세션을 다시 열어 기존 출석 기록을 유지
                    existingSession.reopen(now, currentUser.id());
                    return existingSession;
                })
                .orElseGet(() -> attendanceSessionRepository.save(
                        AttendanceSession.open(
                                lecture,
                                today,
                                period,
                                currentUser.id(),
                                now
                        )
                ));

        // 저장된 세션 ID로 첫 QR을 발급하고 Redis에 보관
        AttendanceQrResponseDTO qr = attendanceQrService.issue(saved.getId());

        // 해당 강의의 ACTIVE 수강생 수를 조회
        long totalEnrollmentCount = enrollmentQueryRepository
                .countActiveEnrollments(lecture.getId());

        long attendedCount = attendanceRepository.countBySessionIdAndStatusIn(
                saved.getId(),
                List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE)
        );

        return new AttendanceSessionOpenResponseDTO(
                saved.getId(),
                lecture.getId(),
                lecture.getCourse().getName(),
                lecture.getSectionNo(),
                saved.getPeriod(),
                saved.getStatus(),
                saved.getOpenedAt(),
                attendedCount,
                totalEnrollmentCount,
                qr
        );
    }


    // qr 갱신
    @Transactional(readOnly = true)
    public AttendanceQrResponseDTO renewQr(
            Long sessionId,
            CurrentUser currentUser
    ) {
        attendanceSessionRepository
                .findByIdAndStatusAndLecture_Professor_User_Id(
                        sessionId,
                        AttendanceSessionStatus.OPEN,
                        currentUser.id()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "QR을 발급할 수 있는 출석 세션이 아닙니다."
                        )
                );

        return attendanceQrService.issue(sessionId);
    }

    // 출석 세션 종료
    @Transactional(rollbackFor = Exception.class)
    public AttendanceSessionCloseResponseDTO close(
            Long sessionId,
            CurrentUser currentUser
    ) {
            AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("출석 세션을 찾을 수 없습니다."));

        Long professorUserId =
                session.getLecture()
                        .getProfessor()
                        .getUser()
                        .getId();

        if (!professorUserId.equals(currentUser.id())) {
            throw new ProfessorLectureAccessDeniedException();
        }

        session.close(LocalDateTime.now());

        return AttendanceSessionCloseResponseDTO.from(session);
    }

    @Transactional(readOnly = true)
    public AttendanceSessionResponseDTO getCurrent(
            Long classId,
            CurrentUser currentUser
    ) {
        return attendanceSessionRepository
                .findFirstByLectureIdAndStatusAndLecture_Professor_User_IdOrderByOpenedAtDesc(
                        classId,
                        AttendanceSessionStatus.OPEN,
                        currentUser.id()
                )
                .map(AttendanceSessionResponseDTO::from)
                .orElse(null); // 진행중인 세션이 없으면 null 반환
    }

    // 출석 세션 현재 참여 인원
    @Transactional(readOnly = true)
    public AttendanceSessionSummaryResponseDTO getSummary(
            Long sessionId,
            CurrentUser currentUser
    ) {
        // 세션 조회 및 담당 강의 교수가 맞는지 확인
        AttendanceSession session = attendanceSessionRepository.findByIdAndLecture_Professor_User_Id(
                sessionId,
                currentUser.id()
        )
                .orElseThrow(ProfessorLectureAccessDeniedException::new);

        // 출석에 참여한 PRESENT, LATE 학생 수 계산
        long attendedCount = attendanceRepository.countBySessionIdAndStatusIn(
                sessionId,
                List.of(
                        AttendanceStatus.PRESENT,
                        AttendanceStatus.LATE
                )
        );

        // 해당 강의를 ACTIVE 상태로 수강 중인 전체 학생 수 계산
        long totalEnrollmentCount = enrollmentQueryRepository.countActiveEnrollments(
                session.getLecture().getId()
        );

        return new AttendanceSessionSummaryResponseDTO(
                session.getId(),
                session.getStatus(),
                attendedCount,
                totalEnrollmentCount
        );
    }

    // 현재 학기 출석 세션 내역 조회
    @Transactional(readOnly = true)
    public PageResponseDTO<AttendanceSessionListResponseDTO> getSessions(
            int page,
            int size,
            CurrentUser currentUser
    ){
        int resolvedPage = Math.max(page, 1);
        int resolvedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                resolvedPage - 1,
                resolvedSize
        );

        // 로그인한 교수의 현재 학기 출석 세션을 최근 순으로 조회
        Page<AttendanceSession> result = attendanceSessionRepository
                .findAllByLecture_Professor_User_IdAndLecture_Semester_CurrentTrueOrderByOpenedAtDesc(
                        currentUser.id(),
                        pageable
                );

        // 각 출석 세션을 화면에 반환할 목록 DTO로 변환
        List<AttendanceSessionListResponseDTO> items =
                result.getContent()
                        .stream()
                        .map(session -> {
                            // 해당 세션에서 PRESENT 또는 LATE인 학생 수 조회
                            long attendedCount = attendanceRepository
                                    .countBySessionIdAndStatusIn(
                                            session.getId(),
                                            List.of(
                                                    AttendanceStatus.PRESENT,
                                                    AttendanceStatus.LATE
                                            )
                                    );

                            // 해당 강의의 전체 ACTIVE 수강생 수 조회
                            long totalEnrollmentCount = enrollmentQueryRepository.countActiveEnrollments(
                                    session.getLecture().getId()
                            );

                            return AttendanceSessionListResponseDTO.of(
                                    session,
                                    attendedCount,
                                    totalEnrollmentCount
                            );
                        }) .toList();

        return new PageResponseDTO<>(
                items,
                result.getTotalElements(),
                resolvedPage,
                resolvedSize,
                result.hasNext()
        );
    }
}
