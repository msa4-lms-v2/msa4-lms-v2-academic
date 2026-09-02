package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCart;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCartRepository;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.global.error.EnrollmentCartAccessDeniedException;
import com.msa4lmsv2academic.global.error.EnrollmentCartConflictException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentCartServiceTest {

    private static final long USER_ID = 10L;
    private static final long STUDENT_ID = 20L;
    private static final long LECTURE_ID = 30L;

    private EnrollmentCartQueryRepository queryRepository;
    private EnrollmentCartRepository cartRepository;
    private EnrollmentCartService service;
    private Student student;
    private Lecture lecture;
    private Semester semester;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        queryRepository = mock(EnrollmentCartQueryRepository.class);
        cartRepository = mock(EnrollmentCartRepository.class);
        service = new EnrollmentCartService(
                queryRepository,
                cartRepository,
                new EnrollmentAcademicStatusValidator()
        );
        student = mock(Student.class);
        lecture = mock(Lecture.class);
        semester = mock(Semester.class);
        currentUser = new CurrentUser(USER_ID, "STUDENT");

        when(student.getId()).thenReturn(STUDENT_ID);
        when(student.getAcademicStatus()).thenReturn(AcademicStatus.ENROLLED);
        when(lecture.getId()).thenReturn(LECTURE_ID);
        when(lecture.getStatus()).thenReturn(LectureStatus.OPEN);
        when(lecture.getSemester()).thenReturn(semester);
        when(queryRepository.findStudentByUserId(USER_ID)).thenReturn(Optional.of(student));
        when(queryRepository.findLecture(LECTURE_ID)).thenReturn(Optional.of(lecture));
        when(cartRepository.saveAndFlush(any(EnrollmentCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void readsCartEvenOutsideEnrollmentPeriod() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(2));
        when(queryRepository.findByStudentUserId(USER_ID, null, null)).thenReturn(List.of());

        var result = service.getMyCart(new EnrollmentCartSearchRequestDTO(null, null), currentUser);

        assertThat(result.totalCredits()).isZero();
        assertThat(result.items()).isEmpty();
        verify(queryRepository).findByStudentUserId(USER_ID, null, null);
    }

    @Test
    void addsLectureDuringEnrollmentPeriodWithoutReservingCapacity() {
        openEnrollmentPeriod();

        var result = service.add(new EnrollmentCartCreateRequestDTO(LECTURE_ID), currentUser);

        assertThat(result.studentId()).isEqualTo(STUDENT_ID);
        assertThat(result.lectureId()).isEqualTo(LECTURE_ID);
        verify(cartRepository).saveAndFlush(any(EnrollmentCart.class));
    }

    @Test
    void rejectsAddOutsideEnrollmentPeriod() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> service.add(new EnrollmentCartCreateRequestDTO(LECTURE_ID), currentUser))
                .isInstanceOf(EnrollmentCartConflictException.class)
                .hasMessageContaining("수강신청 기간");
    }

    @Test
    void rejectsDuplicateCartItem() {
        openEnrollmentPeriod();
        when(queryRepository.existsByStudentAndLecture(STUDENT_ID, LECTURE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.add(new EnrollmentCartCreateRequestDTO(LECTURE_ID), currentUser))
                .isInstanceOf(EnrollmentCartConflictException.class)
                .hasMessageContaining("이미 장바구니");
    }

    @Test
    void rejectsLectureThatIsNotOpen() {
        openEnrollmentPeriod();
        when(lecture.getStatus()).thenReturn(LectureStatus.CLOSED);

        assertThatThrownBy(() -> service.add(new EnrollmentCartCreateRequestDTO(LECTURE_ID), currentUser))
                .isInstanceOf(EnrollmentCartConflictException.class)
                .hasMessageContaining("개설 강의");
    }

    @Test
    void removesOwnedItemDuringEnrollmentPeriod() {
        openEnrollmentPeriod();
        EnrollmentCart cart = EnrollmentCart.create(student, lecture, LocalDateTime.now());
        when(queryRepository.findOwnedItemForUpdate(40L, STUDENT_ID)).thenReturn(Optional.of(cart));

        service.remove(40L, currentUser);

        verify(cartRepository).delete(cart);
    }

    @Test
    void rejectsRemoveOutsideEnrollmentPeriod() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(2));
        EnrollmentCart cart = EnrollmentCart.create(student, lecture, LocalDateTime.now());
        when(queryRepository.findOwnedItemForUpdate(40L, STUDENT_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.remove(40L, currentUser))
                .isInstanceOf(EnrollmentCartConflictException.class)
                .hasMessageContaining("수강신청 기간");
    }

    @Test
    void rejectsProfessorRole() {
        assertThatThrownBy(() -> service.getMyCart(null, new CurrentUser(USER_ID, "PROFESSOR")))
                .isInstanceOf(EnrollmentCartAccessDeniedException.class);
    }

    private void openEnrollmentPeriod() {
        when(semester.getEnrollmentStartAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(semester.getEnrollmentEndAt()).thenReturn(LocalDateTime.now().plusDays(1));
    }
}
