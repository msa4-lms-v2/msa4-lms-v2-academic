package com.msa4lmsv2academic.domain.enrollment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

class EnrollmentCreditLimitRuleEntityTest {

    @Test
    void usesOptimisticLockVersion() throws NoSuchFieldException {
        assertThat(EnrollmentCreditLimitRule.class.getDeclaredField("version")
                .isAnnotationPresent(Version.class)).isTrue();
    }
}
