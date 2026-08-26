package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.RetakeStatus;
import com.msa4lmsv2academic.domain.enrollment.repository.CourseGradeAttemptQueryResult;
import com.msa4lmsv2academic.domain.enrollment.repository.PrerequisiteRetakeRuleQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteCompletionResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeReasonResponseDTO;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PrerequisiteRetakeEvaluatorTest {

    private static final long STUDENT_ID = 8L;
    private static final long COURSE_ID = 20L;

    private PrerequisiteRetakeRuleQueryRepository queryRepository;
    private PrerequisiteRetakeEvaluator evaluator;
    private Course targetCourse;

    @BeforeEach
    void setUp() {
        queryRepository = mock(PrerequisiteRetakeRuleQueryRepository.class);
        evaluator = new PrerequisiteRetakeEvaluator(queryRepository);
        targetCourse = course(COURSE_ID);
        when(queryRepository.findActiveRulesByCourseId(COURSE_ID)).thenReturn(List.of());
    }

    @Test
    void treatsNoHistoryAsFirstEnrollmentWithoutCriteriaSearch() {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of());

        assertThat(result.studentId()).isEqualTo(STUDENT_ID);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.courseCode()).isEqualTo("COURSE-20");
        assertThat(result.courseName()).isEqualTo("교과목20");
        assertThat(result.prerequisites()).isEmpty();
        assertThat(result.retakeCondition().status()).isEqualTo(RetakeStatus.FIRST_ENROLLMENT);
        assertThat(result.retakeCondition().referenceGrade()).isNull();
        assertThat(result.retakeCondition().reason()).isNull();
        assertThat(result.ruleSatisfied()).isTrue();
        assertThat(result.reasons()).isEmpty();
        verify(queryRepository).findActiveRulesByCourseId(COURSE_ID);
        verify(queryRepository).findGradeAttempts(STUDENT_ID, List.of(COURSE_ID));
        verifyNoMoreInteractions(queryRepository);
    }

    @Test
    void treatsOnlyFHistoryAsFirstEnrollment() {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of(opened(COURSE_ID, "F")));

        assertThat(result.retakeCondition().status()).isEqualTo(RetakeStatus.FIRST_ENROLLMENT);
        assertThat(result.retakeCondition().referenceGrade()).isNull();
        assertThat(result.ruleSatisfied()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"C+", "C", "D+", "D"})
    void allowsRetakeForPassingGradesBelowB(String grade) {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of(opened(COURSE_ID, grade)));

        assertThat(result.retakeCondition().status()).isEqualTo(RetakeStatus.RETAKE_ALLOWED);
        assertThat(result.retakeCondition().referenceGrade()).isEqualTo(grade);
        assertThat(result.retakeCondition().satisfied()).isTrue();
        assertThat(result.ruleSatisfied()).isTrue();
        assertThat(result.reasons()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A+", "A", "B+", "B"})
    void blocksRetakeForAnyGradeAtLeastB(String grade) {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of(
                opened(COURSE_ID, "C+"), opened(COURSE_ID, grade), opened(COURSE_ID, "F")
        ));

        assertRejected(result, RetakeStatus.RETAKE_BLOCKED,
                PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE);
        assertThat(result.retakeCondition().referenceGrade()).isEqualTo(grade);
    }

    @Test
    void usesHighestGradeWithinAllowedAndBlockedHistories() {
        PrerequisiteRetakeEvaluationResponseDTO allowed = evaluate(List.of(
                opened(COURSE_ID, "D"), opened(COURSE_ID, "C+"), opened(COURSE_ID, "C")
        ));
        PrerequisiteRetakeEvaluationResponseDTO blocked = evaluate(List.of(
                opened(COURSE_ID, "B"), opened(COURSE_ID, "A+"), opened(COURSE_ID, "A")
        ));

        assertThat(allowed.retakeCondition().referenceGrade()).isEqualTo("C+");
        assertThat(blocked.retakeCondition().referenceGrade()).isEqualTo("A+");
    }

    @Test
    void prioritizesCurrentDraftOverHighGradeButHighGradeOverHistoricalDraft() {
        PrerequisiteRetakeEvaluationResponseDTO current = evaluate(List.of(
                opened(COURSE_ID, "B"), attempt(COURSE_ID, EnrollmentStatus.ACTIVE, GradeStatus.DRAFT, null, true)
        ));
        PrerequisiteRetakeEvaluationResponseDTO historical = evaluate(List.of(
                attempt(COURSE_ID, EnrollmentStatus.ACTIVE, GradeStatus.DRAFT, null, false), opened(COURSE_ID, "B")
        ));

        assertRejected(current, RetakeStatus.ACTIVE_ENROLLMENT_EXISTS,
                PrerequisiteRetakeRuleRejectionReason.ACTIVE_ENROLLMENT_EXISTS);
        assertRejected(historical, RetakeStatus.RETAKE_BLOCKED,
                PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE);
    }

    @ParameterizedTest
    @MethodSource("unresolvedGrades")
    void rejectsUnresolvedGrades(
            GradeStatus gradeStatus,
            String grade,
            RetakeStatus status,
            PrerequisiteRetakeRuleRejectionReason reason
    ) {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of(
                opened(COURSE_ID, "C+"), attempt(COURSE_ID, EnrollmentStatus.ACTIVE, gradeStatus, grade, false)
        ));

        assertRejected(result, status, reason);
        assertThat(result.retakeCondition().referenceGrade()).isEqualTo(grade);
    }

    private static Stream<Arguments> unresolvedGrades() {
        return Stream.of(
                Arguments.of(GradeStatus.DRAFT, null, RetakeStatus.GRADE_PENDING,
                        PrerequisiteRetakeRuleRejectionReason.GRADE_NOT_OPENED),
                Arguments.of(GradeStatus.OPENED, null, RetakeStatus.GRADE_PENDING,
                        PrerequisiteRetakeRuleRejectionReason.GRADE_NOT_ENTERED),
                Arguments.of(GradeStatus.OPENED, "X", RetakeStatus.INVALID_GRADE_DATA,
                        PrerequisiteRetakeRuleRejectionReason.INVALID_GRADE_DATA)
        );
    }

    @Test
    void ignoresCancelledHistoryForRetake() {
        PrerequisiteRetakeEvaluationResponseDTO result = evaluate(List.of(
                attempt(COURSE_ID, EnrollmentStatus.CANCELLED, GradeStatus.DRAFT, null, true),
                attempt(COURSE_ID, EnrollmentStatus.CANCELLED, GradeStatus.OPENED, "A+", false),
                attempt(COURSE_ID, EnrollmentStatus.CANCELLED, GradeStatus.OPENED, "C+", false)
        ));

        assertThat(result.retakeCondition().status()).isEqualTo(RetakeStatus.FIRST_ENROLLMENT);
        assertThat(result.ruleSatisfied()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A+", "A", "B+", "B", "C+", "C", "D+", "D"})
    void acceptsEveryOpenedPassingGradeForPrerequisite(String grade) {
        CoursePrerequisite prerequisite = rule(1L, 10L);
        when(queryRepository.findActiveRulesByCourseId(COURSE_ID)).thenReturn(List.of(prerequisite));
        when(queryRepository.findGradeAttempts(STUDENT_ID, List.of(COURSE_ID, 10L)))
                .thenReturn(List.of(opened(10L, grade)));

        PrerequisiteRetakeEvaluationResponseDTO result = evaluator.evaluate(STUDENT_ID, targetCourse);

        assertThat(result.prerequisiteSatisfied()).isTrue();
        assertThat(result.prerequisites().getFirst().completedGrade()).isEqualTo(grade);
        assertThat(result.prerequisites().getFirst().reason()).isNull();
        assertThat(result.ruleSatisfied()).isTrue();
    }

    @Test
    void requiresAllDirectPrerequisitesAndDeduplicatesCombinedFailureReasons() {
        List<CoursePrerequisite> prerequisites = List.of(rule(1L, 10L), rule(2L, 11L), rule(3L, 12L));
        when(queryRepository.findActiveRulesByCourseId(COURSE_ID)).thenReturn(prerequisites);
        when(queryRepository.findGradeAttempts(STUDENT_ID, List.of(COURSE_ID, 10L, 11L, 12L)))
                .thenReturn(List.of(
                        opened(10L, "D"),
                        opened(11L, "F"),
                        attempt(11L, EnrollmentStatus.CANCELLED, GradeStatus.OPENED, "A", false),
                        attempt(12L, EnrollmentStatus.ACTIVE, GradeStatus.DRAFT, "A", false),
                        opened(12L, null),
                        opened(12L, "X"),
                        opened(COURSE_ID, "B")
                ));

        PrerequisiteRetakeEvaluationResponseDTO result = evaluator.evaluate(STUDENT_ID, targetCourse);

        assertThat(result.prerequisiteSatisfied()).isFalse();
        assertThat(result.prerequisites()).extracting(PrerequisiteCompletionResponseDTO::satisfied)
                .containsExactly(true, false, false);
        assertThat(result.retakeCondition().status()).isEqualTo(RetakeStatus.RETAKE_BLOCKED);
        assertThat(result.ruleSatisfied()).isFalse();
        assertThat(result.reasons()).extracting(PrerequisiteRetakeReasonResponseDTO::code).containsExactly(
                PrerequisiteRetakeRuleRejectionReason.PREREQUISITE_NOT_COMPLETED,
                PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE
        );
        verify(queryRepository).findActiveRulesByCourseId(COURSE_ID);
        verify(queryRepository).findGradeAttempts(STUDENT_ID, List.of(COURSE_ID, 10L, 11L, 12L));
        verifyNoMoreInteractions(queryRepository);
    }

    private PrerequisiteRetakeEvaluationResponseDTO evaluate(List<CourseGradeAttemptQueryResult> attempts) {
        when(queryRepository.findGradeAttempts(STUDENT_ID, List.of(COURSE_ID))).thenReturn(attempts);
        return evaluator.evaluate(STUDENT_ID, targetCourse);
    }

    private void assertRejected(
            PrerequisiteRetakeEvaluationResponseDTO result,
            RetakeStatus status,
            PrerequisiteRetakeRuleRejectionReason reason
    ) {
        assertThat(result.retakeCondition().status()).isEqualTo(status);
        assertThat(result.retakeCondition().satisfied()).isFalse();
        assertThat(result.retakeCondition().reason()).isEqualTo(PrerequisiteRetakeReasonResponseDTO.from(reason));
        assertThat(result.ruleSatisfied()).isFalse();
        assertThat(result.reasons()).containsExactly(PrerequisiteRetakeReasonResponseDTO.from(reason));
    }

    private CourseGradeAttemptQueryResult opened(long courseId, String grade) {
        return attempt(courseId, EnrollmentStatus.ACTIVE, GradeStatus.OPENED, grade, false);
    }

    private CourseGradeAttemptQueryResult attempt(
            long courseId,
            EnrollmentStatus enrollmentStatus,
            GradeStatus gradeStatus,
            String grade,
            boolean currentSemester
    ) {
        return new CourseGradeAttemptQueryResult(
                courseId, courseId * 10, enrollmentStatus, gradeStatus, grade,
                (short) 2026, SemesterTerm.FIRST, currentSemester
        );
    }

    private CoursePrerequisite rule(long ruleId, long prerequisiteCourseId) {
        CoursePrerequisite rule = mock(CoursePrerequisite.class);
        Course prerequisiteCourse = course(prerequisiteCourseId);
        when(rule.getId()).thenReturn(ruleId);
        when(rule.getPrerequisiteCourse()).thenReturn(prerequisiteCourse);
        return rule;
    }

    private Course course(long courseId) {
        Course course = mock(Course.class);
        when(course.getId()).thenReturn(courseId);
        when(course.getCode()).thenReturn("COURSE-" + courseId);
        when(course.getName()).thenReturn("교과목" + courseId);
        return course;
    }
}
