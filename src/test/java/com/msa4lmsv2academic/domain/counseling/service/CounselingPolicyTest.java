package com.msa4lmsv2academic.domain.counseling.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.CounselingStatusConflictException;
import org.junit.jupiter.api.Test;

class CounselingPolicyTest {

    private final CounselingPolicy policy = new CounselingPolicy();

    @Test
    void allowsEnrolledAndOnLeaveStudentsOnly() {
        assertThatCode(() -> policy.requireCounselingAllowed(AcademicStatus.ENROLLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireCounselingAllowed(AcademicStatus.ON_LEAVE))
                .doesNotThrowAnyException();

        for (AcademicStatus status : new AcademicStatus[]{
                AcademicStatus.GRADUATED,
                AcademicStatus.WITHDRAWN,
                AcademicStatus.DISMISSED
        }) {
            assertThatThrownBy(() -> policy.requireCounselingAllowed(status))
                    .isInstanceOf(CounselingStatusConflictException.class);
        }
    }
}
