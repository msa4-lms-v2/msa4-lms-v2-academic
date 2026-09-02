package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentHistory;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCancellationQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentHistoryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentRepository;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.EnrollmentApplicationRejectedException;
import com.msa4lmsv2academic.global.error.EnrollmentCancellationAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentEnrollmentCancellationServiceTest {

    private static final long USER_ID = 10L;
    private static final long STUDENT_ID = 20L;
    private static final long LECTURE_ID = 30L;
    private static final long ENROLLMENT_ID = 40L;

    private EnrollmentCancellationQueryRepository queryRepository;
    private EnrollmentRepository enrollmentRepository;
    private EnrollmentHistoryRepository historyRepository;
    private StudentEnrollmentCancellationService service;
    private Enrollment enrollment;
    private Semester semester;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        queryRepository = mock(EnrollmentCancellationQueryRepository.class);
        enrollmentRepository = mock(EnrollmentRepository.class);
        historyRepository = mock(EnrollmentHistoryRepository.class);
        service = new StudentEnrollmentCancellationService(
                queryRepository,
                enrollmentRepository,
                historyRepository
        );
        Student student = mock(Student.class);
        Lecture lecture = mock(Lecture.class);
        semester = mock(Semester.class);
        when(student.getId()).thenReturn(STUDENT_ID);
        when(lecture.getId()).thenReturn(LECTURE_ID);
        when(lecture.getSemester()).thenReturn(semester);
        enrollment = Enrollment.create(student, lecture, LocalDateTime.of(2026, 8, 20, 9, 0));
        currentUser = new CurrentUser(USER_ID, "STUDENT");
        when(queryRepository.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, USER_ID))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.saveAndFlush(enrollment)).thenReturn(enrollment);
    }

    @Test
    void cancelsActiveOwnedEnrollmentDuringEnrollmentPeriodAndWritesHistory() {
        openEnrollmentPeriod();

        var result = service.cancel(ENROLLMENT_ID, currentUser);

        assertThat(result.studentId()).isEqualTo(STUDENT_ID);
        assertThat(result.lectureId()).isEqualTo(LECTURE_ID);
        assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(result.cancelledAt()).isNotNull();
        verify(enrollmentRepository).saveAndFlush(enrollment);
        verify(historyRepository).saveAndFlush(any(EnrollmentHistory.class));
    }

    @Test
    void rejectsCancellationOutsideEnrollmentPeriodWithoutChangingStatus() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, currentUser))
                .isInstanceOf(EnrollmentApplicationRejectedException.class)
                .hasMessageContaining("수강신청 기간");
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(enrollmentRepository, never()).saveAndFlush(any());
        verify(historyRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAlreadyCancelledEnrollment() {
        enrollment.cancel();

        assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, currentUser))
                .isInstanceOf(EnrollmentApplicationRejectedException.class)
                .hasMessageContaining("이미 취소");
    }

    @Test
    void hidesEnrollmentOwnedByAnotherStudentAsNotFound() {
        when(queryRepository.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, currentUser))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    void rejectsProfessorRole() {
        assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, new CurrentUser(USER_ID, "PROFESSOR")))
                .isInstanceOf(EnrollmentCancellationAccessDeniedException.class);
    }

    private void openEnrollmentPeriod() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(1));
    }
}
