package com.msa4lmsv2academic.domain.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentAcademicStatusRejectionReason;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.EnrollmentAcademicStatusNotAllowedException;
import com.msa4lmsv2academic.global.response.CustomResponseCode;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EnrollmentAcademicStatusValidatorTest {

    private final EnrollmentAcademicStatusValidator validator = new EnrollmentAcademicStatusValidator();

    @Test
    void allowsEnrolledStudent() {
        assertThatCode(() -> validator.validate(AcademicStatus.ENROLLED))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("ineligibleStatuses")
    void rejectsIneligibleStatusWithStandardReason(
            AcademicStatus academicStatus,
            EnrollmentAcademicStatusRejectionReason expectedReason
    ) {
        assertThatThrownBy(() -> validator.validate(academicStatus))
                .isInstanceOfSatisfying(EnrollmentAcademicStatusNotAllowedException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(CustomResponseCode.DUPLICATE_DATA);
                    assertThat(exception.getCurrentStatus()).isEqualTo(academicStatus);
                    assertThat(exception.getReason()).isEqualTo(expectedReason);
                    assertThat(exception.getMessage()).isEqualTo(expectedReason.getMessage());
                });
    }

    private static Stream<Arguments> ineligibleStatuses() {
        return Stream.of(
                Arguments.of(AcademicStatus.ON_LEAVE,
                        EnrollmentAcademicStatusRejectionReason.STUDENT_ON_LEAVE),
                Arguments.of(AcademicStatus.WITHDRAWN,
                        EnrollmentAcademicStatusRejectionReason.STUDENT_WITHDRAWN),
                Arguments.of(AcademicStatus.GRADUATED,
                        EnrollmentAcademicStatusRejectionReason.STUDENT_GRADUATED),
                Arguments.of(AcademicStatus.DISMISSED,
                        EnrollmentAcademicStatusRejectionReason.STUDENT_DISMISSED)
        );
    }
}
