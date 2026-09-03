package com.msa4lmsv2academic.domain.graduation.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GraduationCreditGradePolicyTest {

    @Test
    void treatsMissingLetterGradeAsNotPassing() {
        assertFalse(GraduationCreditGradePolicy.isPassing(null));
    }

    @Test
    void recognizesPassingAndFailingGrades() {
        assertTrue(GraduationCreditGradePolicy.isPassing("A+"));
        assertFalse(GraduationCreditGradePolicy.isPassing("F"));
    }
}
