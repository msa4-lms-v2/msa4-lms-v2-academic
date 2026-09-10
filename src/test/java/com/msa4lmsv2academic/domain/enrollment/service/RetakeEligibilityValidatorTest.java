package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentApplicationQueryRepository;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.EnrollmentRetakeNotAllowedException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RetakeEligibilityValidatorTest {

    private static final long STUDENT_ID = 7L;
    private static final long COURSE_ID = 13L;

    private EnrollmentApplicationQueryRepository queryRepository;
    private RetakeEligibilityValidator validator;
    private Course course;

    @BeforeEach
    void setUp() {
        queryRepository = mock(EnrollmentApplicationQueryRepository.class);
        validator = new RetakeEligibilityValidator(queryRepository);
        course = mock(Course.class);
        when(course.getId()).thenReturn(COURSE_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C+", "C", "D+", "D"})
    void allowsConfiguredRetakeGrades(String grade) {
        when(queryRepository.findNonCancelledCourseEnrollments(STUDENT_ID, COURSE_ID))
                .thenReturn(List.of(attempt(EnrollmentStatus.ACTIVE, GradeStatus.OPENED, grade, false)));

        assertThat(validator.validateAndCount(STUDENT_ID, course)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A+", "A", "B+", "B"})
    void blocksAnyGradeAtLeastB(String grade) {
        when(queryRepository.findNonCancelledCourseEnrollments(STUDENT_ID, COURSE_ID))
                .thenReturn(List.of(attempt(EnrollmentStatus.ACTIVE, GradeStatus.OPENED, grade, false)));

        assertThatThrownBy(() -> validator.validateAndCount(STUDENT_ID, course))
                .isInstanceOfSatisfying(EnrollmentRetakeNotAllowedException.class, exception ->
                        assertThat(exception.getReason()).isEqualTo(RetakeRejectionReason.RETAKE_BLOCKED_HIGH_GRADE));
    }

    @Test
    void ignoresCancelledHighGradeHistory() {
        when(queryRepository.findNonCancelledCourseEnrollments(STUDENT_ID, COURSE_ID)).thenReturn(List.of());

        assertThatCode(() -> validator.validateAndCount(STUDENT_ID, course)).doesNotThrowAnyException();
    }

    private Enrollment attempt(
            EnrollmentStatus enrollmentStatus,
            GradeStatus gradeStatus,
            String grade,
            boolean currentSemester
    ) {
        Enrollment enrollment = mock(Enrollment.class);
        Lecture lecture = mock(Lecture.class);
        Semester semester = mock(Semester.class);
        when(enrollment.getStatus()).thenReturn(enrollmentStatus);
        when(enrollment.getGradeStatus()).thenReturn(gradeStatus);
        when(enrollment.getLetterGrade()).thenReturn(grade);
        when(enrollment.getLecture()).thenReturn(lecture);
        when(lecture.getSemester()).thenReturn(semester);
        when(semester.isCurrent()).thenReturn(currentSemester);
        return enrollment;
    }
}
