package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.enrollment.entity.PrerequisiteRetakeRuleRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditQueryRepository;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeEvaluationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeReasonResponseDTO;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentPrerequisiteRetakeNotAllowedException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

class EnrollmentCourseRuleValidatorTest {

    private static final long STUDENT_ID = 8L;
    private static final long SEMESTER_ID = 23L;

    private EnrollmentCreditLimitRuleRepository ruleRepository;
    private EnrollmentCreditQueryRepository creditQueryRepository;
    private PrerequisiteRetakeEvaluator prerequisiteRetakeEvaluator;
    private EnrollmentCourseRuleValidator validator;
    private Lecture lecture;
    private Course course;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(EnrollmentCreditLimitRuleRepository.class);
        creditQueryRepository = mock(EnrollmentCreditQueryRepository.class);
        prerequisiteRetakeEvaluator = mock(PrerequisiteRetakeEvaluator.class);
        validator = new EnrollmentCourseRuleValidator(ruleRepository, creditQueryRepository, prerequisiteRetakeEvaluator);
        lecture = mock(Lecture.class);
        course = mock(Course.class);
        Semester semester = mock(Semester.class);
        when(lecture.getSemester()).thenReturn(semester);
        when(semester.getId()).thenReturn(SEMESTER_ID);
        when(lecture.getCourse()).thenReturn(course);
        when(course.getCredits()).thenReturn((byte) 3);
        when(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID))
                .thenReturn(Optional.of(EnrollmentCreditLimitRule.create(semester, 18)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 14, 15})
    void allowsTotalAtOrBelowLimitAndDelegatesOnlyAfterCreditValidation(long activeCredits) {
        when(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).thenReturn(activeCredits);
        when(prerequisiteRetakeEvaluator.evaluate(STUDENT_ID, course)).thenReturn(evaluation(List.of()));

        assertThatCode(() -> validator.validate(STUDENT_ID, lecture)).doesNotThrowAnyException();

        InOrder order = inOrder(ruleRepository, creditQueryRepository, prerequisiteRetakeEvaluator);
        order.verify(ruleRepository).findBySemesterIdAndActiveTrue(SEMESTER_ID);
        order.verify(creditQueryRepository).sumActiveCredits(STUDENT_ID, SEMESTER_ID);
        order.verify(prerequisiteRetakeEvaluator).evaluate(STUDENT_ID, course);
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @ValueSource(longs = {16, 18, 130})
    void rejectsExcessWithoutEvaluatingPrerequisites(long activeCredits) {
        when(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).thenReturn(activeCredits);

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentCreditLimitNotAllowedException.class, exception -> {
                    assertThat(exception.getReason()).isEqualTo(EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_EXCEEDED);
                    assertThat(exception.getCode()).isEqualTo(CustomResponseCode.DUPLICATE_DATA);
                });
        verifyNoInteractions(prerequisiteRetakeEvaluator);
    }

    @Test
    void rejectsMissingActiveRuleWithoutFallbackOrFurtherQueries() {
        when(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentCreditLimitNotAllowedException.class, exception -> {
                    assertThat(exception.getReason())
                            .isEqualTo(EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_RULE_NOT_CONFIGURED);
                    assertThat(exception.getCode()).isEqualTo(CustomResponseCode.DUPLICATE_DATA);
                });
        verifyNoInteractions(creditQueryRepository, prerequisiteRetakeEvaluator);
    }

    @Test
    void usesConfiguredLimitInsteadOfHardcodedEighteenCredits() {
        EnrollmentCreditLimitRule rule = EnrollmentCreditLimitRule.create(lecture.getSemester(), 21);
        when(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID))
                .thenReturn(Optional.of(rule));
        when(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).thenReturn(18L);
        when(prerequisiteRetakeEvaluator.evaluate(STUDENT_ID, course)).thenReturn(evaluation(List.of()));

        assertThatCode(() -> validator.validate(STUDENT_ID, lecture)).doesNotThrowAnyException();
        verify(prerequisiteRetakeEvaluator).evaluate(STUDENT_ID, course);
    }

    @ParameterizedTest
    @EnumSource(PrerequisiteRetakeRuleRejectionReason.class)
    void preservesEveryExistingPrerequisiteAndRetakeReason(PrerequisiteRetakeRuleRejectionReason reason) {
        when(prerequisiteRetakeEvaluator.evaluate(STUDENT_ID, course)).thenReturn(evaluation(List.of(reason)));

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentPrerequisiteRetakeNotAllowedException.class, exception -> {
                    assertThat(exception.getReasons()).containsExactly(reason);
                    assertThat(exception.getMessage()).isEqualTo(reason.getMessage());
                    assertThat(exception.getCode()).isEqualTo(CustomResponseCode.DUPLICATE_DATA);
                });
    }

    @Test
    void preservesMultipleReasons() {
        List<PrerequisiteRetakeRuleRejectionReason> reasons = List.of(
                PrerequisiteRetakeRuleRejectionReason.PREREQUISITE_NOT_COMPLETED,
                PrerequisiteRetakeRuleRejectionReason.RETAKE_BLOCKED_HIGH_GRADE
        );
        when(prerequisiteRetakeEvaluator.evaluate(STUDENT_ID, course)).thenReturn(evaluation(reasons));

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentPrerequisiteRetakeNotAllowedException.class,
                        exception -> assertThat(exception.getReasons()).containsExactlyElementsOf(reasons));
    }

    private PrerequisiteRetakeEvaluationResponseDTO evaluation(List<PrerequisiteRetakeRuleRejectionReason> reasons) {
        return new PrerequisiteRetakeEvaluationResponseDTO(
                STUDENT_ID, 20L, "CSE3001", "운영체제", reasons.isEmpty(), List.of(), null, reasons.isEmpty(),
                reasons.stream().map(PrerequisiteRetakeReasonResponseDTO::from).toList()
        );
    }
}
