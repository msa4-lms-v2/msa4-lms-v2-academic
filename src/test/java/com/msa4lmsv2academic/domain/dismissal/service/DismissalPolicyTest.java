package com.msa4lmsv2academic.domain.dismissal.service;

import static org.assertj.core.api.Assertions.*;
import com.msa4lmsv2academic.domain.dismissal.entity.DismissalReasonType;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.global.error.DismissalConflictException;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class DismissalPolicyTest {
    @TestFactory Stream<DynamicTest> reasonAndCurrentAcademicStatusMatrix() {
        var policy = new DismissalPolicy();
        return Arrays.stream(DismissalReasonType.values()).flatMap(reason ->
                Arrays.stream(AcademicStatus.values()).map(status -> DynamicTest.dynamicTest(reason + "/" + status, () -> {
                    boolean allowed = status == AcademicStatus.ON_LEAVE
                            || (status == AcademicStatus.ENROLLED && reason != DismissalReasonType.LEAVE_EXPIRED);
                    if (allowed) assertThatCode(() -> policy.validateAcademicStatus(status, reason)).doesNotThrowAnyException();
                    else assertThatThrownBy(() -> policy.validateAcademicStatus(status, reason)).isInstanceOf(DismissalConflictException.class);
                })));
    }
}
