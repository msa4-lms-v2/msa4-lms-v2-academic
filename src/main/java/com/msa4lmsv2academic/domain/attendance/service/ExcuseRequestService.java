package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseRequestResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentExcuseEligibilityService;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.DuplicateExcuseRequestException;
import com.msa4lmsv2academic.global.error.EnrollmentNotFoundException;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InvalidExcuseRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseRequestService {

    private static final int APPLICATION_DEADLINE_DAYS = 7;
    private static final int MAX_REASON_LENGTH = 500;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ExcuseRequestRepository excuseRequestRepository;
    private final EnrollmentExcuseEligibilityService enrollmentEligibilityService;
    private final AttendancePolicy attendancePolicy;

    @Transactional
    public ExcuseRequestResponseDTO create(
            ExcuseRequestCreateRequestDTO request,
            CurrentUser currentUser
    ) {
        validateStudent(currentUser);
        validateRequest(request);

        Enrollment enrollment = enrollmentEligibilityService
                .findOwnedEnrollmentForUpdate(request.enrollmentId(), currentUser.id())
                .orElseThrow(EnrollmentNotFoundException::new);
        validateEnrollment(enrollment);
        validateLectureDate(enrollment.getLecture().getSemester(), request.lectureDate());
        validateLectureSchedule(enrollment, request.lectureDate(), request.period());
        validateDuplicate(enrollment.getId(), request.lectureDate(), request.period());

        ExcuseRequest excuseRequest = ExcuseRequest.create(
                enrollment,
                request.lectureDate(),
                request.period(),
                request.reason().trim()
        );
        try {
            return ExcuseRequestResponseDTO.from(excuseRequestRepository.saveAndFlush(excuseRequest));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateExcuseRequestException();
        }
    }

    private void validateStudent(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || !"STUDENT".equals(currentUser.role())) {
            throw new ExcuseRequestAccessDeniedException("학생만 공결을 신청할 수 있습니다.");
        }
    }

    private void validateRequest(ExcuseRequestCreateRequestDTO request) {
        if (request == null || request.enrollmentId() == null || request.enrollmentId() <= 0
                || request.lectureDate() == null || request.period() == null
                || request.period() < 1 || request.period() > 20
                || request.reason() == null || request.reason().isBlank()
                || request.reason().trim().length() > MAX_REASON_LENGTH) {
            throw new InvalidExcuseRequestException("공결 신청 필수값을 확인해 주세요.");
        }
    }

    private void validateEnrollment(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidExcuseRequestException("활성 상태인 수강에만 공결을 신청할 수 있습니다.");
        }
        attendancePolicy.requireExcuseRequestAllowed(enrollment.getStudent().getAcademicStatus());
    }

    private void validateLectureDate(Semester semester, LocalDate lectureDate) {
        if (semester.getStartDate() == null || semester.getEndDate() == null
                || lectureDate.isBefore(semester.getStartDate()) || lectureDate.isAfter(semester.getEndDate())) {
            throw new InvalidExcuseRequestException("학기 수업 기간에 포함된 날짜만 신청할 수 있습니다.");
        }

        LocalDate today = LocalDate.now(KST);
        if (lectureDate.isAfter(today)) {
            throw new InvalidExcuseRequestException("미래 수업에 대해서는 공결을 신청할 수 없습니다.");
        }
        if (lectureDate.plusDays(APPLICATION_DEADLINE_DAYS).isBefore(today)) {
            throw new InvalidExcuseRequestException("공결은 결석한 수업일로부터 7일 이내에 신청해야 합니다.");
        }
    }

    private void validateLectureSchedule(Enrollment enrollment, LocalDate lectureDate, byte period) {
        LectureDayOfWeek dayOfWeek = toLectureDayOfWeek(lectureDate.getDayOfWeek());
        boolean scheduled = dayOfWeek != null && enrollmentEligibilityService.hasLectureSchedule(
                enrollment.getLecture().getId(),
                dayOfWeek,
                period
        );
        if (!scheduled) {
            throw new InvalidExcuseRequestException("해당 강의의 실제 수업 요일과 교시에만 신청할 수 있습니다.");
        }
    }

    private void validateDuplicate(Long enrollmentId, LocalDate lectureDate, byte period) {
        if (excuseRequestRepository.existsByEnrollmentIdAndLectureDateAndPeriod(
                enrollmentId,
                lectureDate,
                period
        )) {
            throw new DuplicateExcuseRequestException();
        }
    }

    private LectureDayOfWeek toLectureDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> LectureDayOfWeek.MON;
            case TUESDAY -> LectureDayOfWeek.TUE;
            case WEDNESDAY -> LectureDayOfWeek.WED;
            case THURSDAY -> LectureDayOfWeek.THU;
            case FRIDAY -> LectureDayOfWeek.FRI;
            case SATURDAY, SUNDAY -> null;
        };
    }
}
