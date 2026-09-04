package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.AttendanceStateConflictException;
import org.junit.jupiter.api.Test;

class AttendancePolicyTest {

    private final AttendancePolicy policy = new AttendancePolicy();

    @Test
    void allowsOnlyEnrolledStudentToCheckInAndRequestExcuse() {
        assertThatCode(() -> policy.requireCheckInAllowed(AcademicStatus.ENROLLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireExcuseRequestAllowed(AcademicStatus.ENROLLED))
                .doesNotThrowAnyException();

        for (AcademicStatus status : AcademicStatus.values()) {
            if (status == AcademicStatus.ENROLLED) {
                continue;
            }
            assertThatThrownBy(() -> policy.requireCheckInAllowed(status))
                    .isInstanceOf(AttendanceStateConflictException.class);
            assertThatThrownBy(() -> policy.requireExcuseRequestAllowed(status))
                    .isInstanceOf(AttendanceStateConflictException.class);
        }
    }
}
