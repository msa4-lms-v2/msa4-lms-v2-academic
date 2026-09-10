package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCourseAttemptLimitRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRejectionReason;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditLimitRuleRepository;
import com.msa4lmsv2academic.domain.enrollment.repository.EnrollmentCreditQueryRepository;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.global.error.EnrollmentCourseAttemptLimitNotAllowedException;
import com.msa4lmsv2academic.global.error.EnrollmentCreditLimitNotAllowedException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

class EnrollmentCourseRuleValidatorTest {

    private static final long STUDENT_ID = 8L;
    private static final long SEMESTER_ID = 23L;
    private static final long COURSE_ID = 45L;

    private EnrollmentCreditLimitRuleRepository ruleRepository;
    private EnrollmentCreditQueryRepository creditQueryRepository;
    private RetakeEligibilityValidator retakeEligibilityValidator;
    private EnrollmentCourseRuleValidator validator;
    private Lecture lecture;
    private Course course;

    @BeforeEach
    void setUp() {
        ruleRepository = mock(EnrollmentCreditLimitRuleRepository.class);
        creditQueryRepository = mock(EnrollmentCreditQueryRepository.class);
        retakeEligibilityValidator = mock(RetakeEligibilityValidator.class);
        validator = new EnrollmentCourseRuleValidator(ruleRepository, creditQueryRepository, retakeEligibilityValidator);
        lecture = mock(Lecture.class);
        course = mock(Course.class);
        Semester semester = mock(Semester.class);
        when(lecture.getSemester()).thenReturn(semester);
        when(semester.getId()).thenReturn(SEMESTER_ID);
        when(lecture.getCourse()).thenReturn(course);
        when(course.getId()).thenReturn(COURSE_ID);
        when(course.getCredits()).thenReturn((byte) 3);
        when(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID))
                .thenReturn(Optional.of(EnrollmentCreditLimitRule.create(semester, 18)));
        when(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).thenReturn(0L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void allowsFirstAndSecondAttemptAfterRetakePolicyPasses(long attemptCount) {
        when(retakeEligibilityValidator.validateAndCount(STUDENT_ID, course)).thenReturn(attemptCount);

        assertThatCode(() -> validator.validate(STUDENT_ID, lecture)).doesNotThrowAnyException();

        InOrder order = inOrder(ruleRepository, creditQueryRepository, retakeEligibilityValidator);
        order.verify(ruleRepository).findBySemesterIdAndActiveTrue(SEMESTER_ID);
        order.verify(creditQueryRepository).sumActiveCredits(STUDENT_ID, SEMESTER_ID);
        order.verify(retakeEligibilityValidator).validateAndCount(STUDENT_ID, course);
    }

    @ParameterizedTest
    @ValueSource(longs = {2, 3})
    void rejectsThirdOrLaterAttempt(long attemptCount) {
        when(retakeEligibilityValidator.validateAndCount(STUDENT_ID, course)).thenReturn(attemptCount);

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentCourseAttemptLimitNotAllowedException.class, exception ->
                        assertThat(exception.getReason())
                                .isEqualTo(EnrollmentCourseAttemptLimitRejectionReason.COURSE_ATTEMPT_LIMIT_EXCEEDED));
    }

    @Test
    void rejectsExcessCreditBeforeRetakeOrAttemptLimitQuery() {
        when(creditQueryRepository.sumActiveCredits(STUDENT_ID, SEMESTER_ID)).thenReturn(16L);

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentCreditLimitNotAllowedException.class, exception ->
                        assertThat(exception.getReason())
                                .isEqualTo(EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_EXCEEDED));

        verifyNoInteractions(retakeEligibilityValidator);
    }

    @Test
    void rejectsMissingActiveCreditRuleWithoutFurtherQueries() {
        when(ruleRepository.findBySemesterIdAndActiveTrue(SEMESTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(STUDENT_ID, lecture))
                .isInstanceOfSatisfying(EnrollmentCreditLimitNotAllowedException.class, exception ->
                        assertThat(exception.getReason())
                                .isEqualTo(EnrollmentCreditLimitRejectionReason.CREDIT_LIMIT_RULE_NOT_CONFIGURED));

        verifyNoInteractions(creditQueryRepository, retakeEligibilityValidator);
    }
}
