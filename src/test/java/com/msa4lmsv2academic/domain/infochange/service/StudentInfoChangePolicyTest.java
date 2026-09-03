package com.msa4lmsv2academic.domain.infochange.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.InfoChangeRequestStateConflictException;
import org.junit.jupiter.api.Test;

class StudentInfoChangePolicyTest {

    private final StudentInfoChangePolicy policy = new StudentInfoChangePolicy();

    @Test
    void allowsEnrolledAndOnLeaveStudentsOnly() {
        assertThatCode(() -> policy.requireRequestAllowed(AcademicStatus.ENROLLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireRequestAllowed(AcademicStatus.ON_LEAVE))
                .doesNotThrowAnyException();

        for (AcademicStatus status : new AcademicStatus[]{
                AcademicStatus.GRADUATED,
                AcademicStatus.WITHDRAWN,
                AcademicStatus.DISMISSED
        }) {
            assertThatThrownBy(() -> policy.requireRequestAllowed(status))
                    .isInstanceOf(InfoChangeRequestStateConflictException.class);
        }
    }
}
