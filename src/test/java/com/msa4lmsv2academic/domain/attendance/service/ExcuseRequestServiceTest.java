package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.attendance.request.ExcuseRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentExcuseEligibilityService;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.AttendanceStateConflictException;
import com.msa4lmsv2academic.global.error.DuplicateExcuseRequestException;
import com.msa4lmsv2academic.global.error.EnrollmentNotFoundException;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.InvalidExcuseRequestException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ExcuseRequestServiceTest {

    private static final long USER_ID = 10L;
    private static final long ENROLLMENT_ID = 20L;
    private static final long LECTURE_ID = 30L;
    private static final long REQUEST_ID = 40L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private ExcuseRequestRepository excuseRequestRepository;
    private EnrollmentExcuseEligibilityService enrollmentEligibilityService;
    private ExcuseRequestService service;
    private Enrollment enrollment;
    private Student student;
    private Lecture lecture;
    private Semester semester;
    private CurrentUser currentUser;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        excuseRequestRepository = mock(ExcuseRequestRepository.class);
        enrollmentEligibilityService = mock(EnrollmentExcuseEligibilityService.class);
        service = new ExcuseRequestService(
                excuseRequestRepository,
                enrollmentEligibilityService,
                new AttendancePolicy()
        );

        enrollment = mock(Enrollment.class);
        student = mock(Student.class);
        lecture = mock(Lecture.class);
        semester = mock(Semester.class);
        currentUser = new CurrentUser(USER_ID, "STUDENT");
        today = LocalDate.now(KST);

        when(enrollment.getId()).thenReturn(ENROLLMENT_ID);
        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
        when(enrollment.getStudent()).thenReturn(student);
        when(student.getAcademicStatus()).thenReturn(AcademicStatus.ENROLLED);
        when(enrollment.getLecture()).thenReturn(lecture);
        when(lecture.getId()).thenReturn(LECTURE_ID);
        when(lecture.getSemester()).thenReturn(semester);
        when(semester.getStartDate()).thenReturn(today.minusMonths(1));
        when(semester.getEndDate()).thenReturn(today.plusMonths(1));
        when(enrollmentEligibilityService.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, USER_ID))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentEligibilityService.hasLectureSchedule(
                eq(LECTURE_ID), any(LectureDayOfWeek.class), anyByte()
        )).thenReturn(true);
    }

    @Test
    void createsPendingRequestForOwnedActiveEnrollmentWithinSevenDays() {
        LocalDate lectureDate = recentWeekday(today.minusDays(1));
        ExcuseRequest saved = savedRequest(lectureDate, (byte) 2, "병원 진료");
        when(excuseRequestRepository.saveAndFlush(any(ExcuseRequest.class))).thenReturn(saved);

        var response = service.create(request(lectureDate, (byte) 2, "  병원 진료  "), currentUser);

        assertThat(response.id()).isEqualTo(REQUEST_ID);
        assertThat(response.enrollmentId()).isEqualTo(ENROLLMENT_ID);
        assertThat(response.status()).isEqualTo(ExcuseRequestStatus.PENDING);
        assertThat(response.reason()).isEqualTo("병원 진료");
        verify(excuseRequestRepository).saveAndFlush(any(ExcuseRequest.class));
    }

    @Test
    void allowsApplicationOnSeventhDayAfterLecture() {
        LocalDate lectureDate = today.minusDays(7);
        ExcuseRequest saved = savedRequest(lectureDate, (byte) 1, "진료");
        when(excuseRequestRepository.saveAndFlush(any(ExcuseRequest.class)))
                .thenReturn(saved);

        var response = service.create(request(lectureDate, (byte) 1, "진료"), currentUser);

        assertThat(response.id()).isEqualTo(REQUEST_ID);
    }

    @Test
    void rejectsApplicationAfterSevenDayDeadline() {
        LocalDate lectureDate = today.minusDays(8);

        assertThatThrownBy(() -> service.create(request(lectureDate, (byte) 1, "진료"), currentUser))
                .isInstanceOf(InvalidExcuseRequestException.class)
                .hasMessageContaining("7일 이내");
        verify(excuseRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsFutureLectureAndDateOutsideSemester() {
        assertThatThrownBy(() -> service.create(request(today.plusDays(1), (byte) 1, "진료"), currentUser))
                .isInstanceOf(InvalidExcuseRequestException.class)
                .hasMessageContaining("미래 수업");

        when(semester.getStartDate()).thenReturn(today.plusDays(1));
        assertThatThrownBy(() -> service.create(request(today, (byte) 1, "진료"), currentUser))
                .isInstanceOf(InvalidExcuseRequestException.class)
                .hasMessageContaining("학기 수업 기간");
    }

    @Test
    void rejectsCancelledEnrollmentAndStudentWhoIsNotEnrolled() {
        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.CANCELLED);
        assertThatThrownBy(() -> service.create(request(today, (byte) 1, "진료"), currentUser))
                .isInstanceOf(InvalidExcuseRequestException.class)
                .hasMessageContaining("활성 상태");

        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
        when(student.getAcademicStatus()).thenReturn(AcademicStatus.ON_LEAVE);
        assertThatThrownBy(() -> service.create(request(today, (byte) 1, "진료"), currentUser))
                .isInstanceOf(AttendanceStateConflictException.class)
                .hasMessageContaining("재학 상태");
    }

    @Test
    void rejectsDateOrPeriodThatDoesNotMatchLectureSchedule() {
        when(enrollmentEligibilityService.hasLectureSchedule(anyLong(), any(), anyByte())).thenReturn(false);

        assertThatThrownBy(() -> service.create(request(recentWeekday(today), (byte) 3, "진료"), currentUser))
                .isInstanceOf(InvalidExcuseRequestException.class)
                .hasMessageContaining("실제 수업 요일과 교시");
    }

    @Test
    void rejectsDuplicateRequestBeforeInsertAndConvertsDatabaseRace() {
        LocalDate lectureDate = recentWeekday(today);
        when(excuseRequestRepository.existsByEnrollmentIdAndLectureDateAndPeriod(
                ENROLLMENT_ID, lectureDate, (byte) 1
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(lectureDate, (byte) 1, "진료"), currentUser))
                .isInstanceOf(DuplicateExcuseRequestException.class);

        when(excuseRequestRepository.existsByEnrollmentIdAndLectureDateAndPeriod(
                ENROLLMENT_ID, lectureDate, (byte) 1
        )).thenReturn(false);
        when(excuseRequestRepository.saveAndFlush(any(ExcuseRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.create(request(lectureDate, (byte) 1, "진료"), currentUser))
                .isInstanceOf(DuplicateExcuseRequestException.class);
    }

    @Test
    void rejectsNonStudentAndHidesEnrollmentOwnedByAnotherStudent() {
        assertThatThrownBy(() -> service.create(
                request(today, (byte) 1, "진료"),
                new CurrentUser(USER_ID, "PROFESSOR")
        )).isInstanceOf(ExcuseRequestAccessDeniedException.class);

        when(enrollmentEligibilityService.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, USER_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request(today, (byte) 1, "진료"), currentUser))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    private ExcuseRequestCreateRequestDTO request(LocalDate lectureDate, byte period, String reason) {
        return new ExcuseRequestCreateRequestDTO(ENROLLMENT_ID, lectureDate, period, reason);
    }

    private ExcuseRequest savedRequest(LocalDate lectureDate, byte period, String reason) {
        ExcuseRequest saved = mock(ExcuseRequest.class);
        when(saved.getId()).thenReturn(REQUEST_ID);
        when(saved.getEnrollment()).thenReturn(enrollment);
        when(saved.getLectureDate()).thenReturn(lectureDate);
        when(saved.getPeriod()).thenReturn(period);
        when(saved.getReason()).thenReturn(reason);
        when(saved.getStatus()).thenReturn(ExcuseRequestStatus.PENDING);
        when(saved.getCreatedAt()).thenReturn(LocalDateTime.now(KST));
        return saved;
    }

    private LocalDate recentWeekday(LocalDate candidate) {
        LocalDate result = candidate;
        while (result.getDayOfWeek().getValue() > 5) {
            result = result.minusDays(1);
        }
        return result;
    }
}
