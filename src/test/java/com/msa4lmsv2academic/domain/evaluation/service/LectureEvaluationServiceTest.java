package com.msa4lmsv2academic.domain.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import com.msa4lmsv2academic.domain.evaluation.repository.LectureEvaluationEnrollmentRepository;
import com.msa4lmsv2academic.domain.evaluation.repository.LectureEvaluationRepository;
import com.msa4lmsv2academic.domain.evaluation.request.LectureEvaluationSubmitRequestDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.InvalidLectureEvaluationRequestException;
import com.msa4lmsv2academic.global.error.LectureEvaluationAccessDeniedException;
import com.msa4lmsv2academic.global.error.LectureEvaluationConflictException;
import com.msa4lmsv2academic.global.error.LectureEvaluationEnrollmentNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class LectureEvaluationServiceTest {

    private static final Long STUDENT_USER_ID = 10L;
    private static final Long ENROLLMENT_ID = 20L;
    private static final Long LECTURE_ID = 30L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 10, 14, 30);

    private LectureEvaluationRepository evaluationRepository;
    private LectureEvaluationEnrollmentRepository enrollmentRepository;
    private LectureEvaluationTimeProvider timeProvider;
    private LectureEvaluationService service;
    private Enrollment enrollment;
    private Semester semester;

    @BeforeEach
    void setUp() {
        evaluationRepository = mock(LectureEvaluationRepository.class);
        enrollmentRepository = mock(LectureEvaluationEnrollmentRepository.class);
        timeProvider = mock(LectureEvaluationTimeProvider.class);
        service = new LectureEvaluationService(evaluationRepository, enrollmentRepository, timeProvider);

        enrollment = mock(Enrollment.class);
        Lecture lecture = mock(Lecture.class);
        semester = mock(Semester.class);
        when(enrollment.getId()).thenReturn(ENROLLMENT_ID);
        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
        when(enrollment.getLecture()).thenReturn(lecture);
        when(lecture.getId()).thenReturn(LECTURE_ID);
        when(lecture.getSemester()).thenReturn(semester);
        when(enrollmentRepository.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, STUDENT_USER_ID))
                .thenReturn(Optional.of(enrollment));
        when(timeProvider.now()).thenReturn(NOW);
        when(semester.isEvaluationOpenAt(NOW)).thenReturn(true);
        when(evaluationRepository.saveAndFlush(any(LectureEvaluation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submitsEvaluationForOwnedActiveEnrollmentDuringEvaluationPeriod() {
        var response = service.submit(validRequest(), student());

        assertThat(response.enrollmentId()).isEqualTo(ENROLLMENT_ID);
        assertThat(response.lectureId()).isEqualTo(LECTURE_ID);
        assertThat(response.submittedAt()).isEqualTo(NOW);
        verify(evaluationRepository).saveAndFlush(any(LectureEvaluation.class));
    }

    @Test
    void rejectsProfessorAndMissingStudentPrincipal() {
        assertThatThrownBy(() -> service.submit(validRequest(), new CurrentUser(11L, "PROFESSOR")))
                .isInstanceOf(LectureEvaluationAccessDeniedException.class);
        assertThatThrownBy(() -> service.submit(validRequest(), null))
                .isInstanceOf(LectureEvaluationAccessDeniedException.class);
        verify(enrollmentRepository, never()).findOwnedEnrollmentForUpdate(any(), any());
    }

    @Test
    void hidesEnrollmentOwnedByAnotherStudent() {
        when(enrollmentRepository.findOwnedEnrollmentForUpdate(ENROLLMENT_ID, STUDENT_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(validRequest(), student()))
                .isInstanceOf(LectureEvaluationEnrollmentNotFoundException.class);
    }

    @Test
    void rejectsCancelledEnrollmentAndClosedEvaluationPeriod() {
        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.CANCELLED);
        assertThatThrownBy(() -> service.submit(validRequest(), student()))
                .isInstanceOf(LectureEvaluationConflictException.class)
                .hasMessageContaining("수강 중");

        when(enrollment.getStatus()).thenReturn(EnrollmentStatus.ACTIVE);
        when(semester.isEvaluationOpenAt(NOW)).thenReturn(false);
        assertThatThrownBy(() -> service.submit(validRequest(), student()))
                .isInstanceOf(LectureEvaluationConflictException.class)
                .hasMessageContaining("강의평가 기간");
    }

    @Test
    void rejectsDuplicateBeforeInsertAndConvertsDatabaseRace() {
        when(evaluationRepository.existsByEnrollment_Id(ENROLLMENT_ID)).thenReturn(true);
        assertThatThrownBy(() -> service.submit(validRequest(), student()))
                .isInstanceOf(LectureEvaluationConflictException.class)
                .hasMessageContaining("이미 제출");

        when(evaluationRepository.existsByEnrollment_Id(ENROLLMENT_ID)).thenReturn(false);
        when(evaluationRepository.saveAndFlush(any(LectureEvaluation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThatThrownBy(() -> service.submit(validRequest(), student()))
                .isInstanceOf(LectureEvaluationConflictException.class)
                .hasMessageContaining("이미 제출");
    }

    @Test
    void rejectsInvalidRatingAndOverlongCommentOutsideController() {
        LectureEvaluationSubmitRequestDTO invalidRating = new LectureEvaluationSubmitRequestDTO(
                ENROLLMENT_ID,
                Map.of("CONTENT_QUALITY", 6),
                null
        );
        assertThatThrownBy(() -> service.submit(invalidRating, student()))
                .isInstanceOf(InvalidLectureEvaluationRequestException.class)
                .hasMessageContaining("1점부터 5점");

        LectureEvaluationSubmitRequestDTO overlongComment = new LectureEvaluationSubmitRequestDTO(
                ENROLLMENT_ID,
                Map.of("CONTENT_QUALITY", 5),
                "가".repeat(2001)
        );
        assertThatThrownBy(() -> service.submit(overlongComment, student()))
                .isInstanceOf(InvalidLectureEvaluationRequestException.class)
                .hasMessageContaining("2000자");
    }

    @Test
    void copiesRatingsAndNormalizesBlankCommentBeforeSaving() {
        Map<String, Integer> ratings = new LinkedHashMap<>();
        ratings.put("CONTENT_QUALITY", 5);
        LectureEvaluationSubmitRequestDTO request = new LectureEvaluationSubmitRequestDTO(
                ENROLLMENT_ID,
                ratings,
                "   "
        );

        service.submit(request, student());
        ratings.put("CONTENT_QUALITY", 1);

        var captor = org.mockito.ArgumentCaptor.forClass(LectureEvaluation.class);
        verify(evaluationRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRatings()).containsEntry("CONTENT_QUALITY", 5);
        assertThat(captor.getValue().getComment()).isNull();
    }

    private LectureEvaluationSubmitRequestDTO validRequest() {
        return new LectureEvaluationSubmitRequestDTO(
                ENROLLMENT_ID,
                Map.of("CONTENT_QUALITY", 5, "DELIVERY_CLARITY", 4),
                "실습 예제가 좋았습니다."
        );
    }

    private CurrentUser student() {
        return new CurrentUser(STUDENT_USER_ID, "STUDENT");
    }
}
