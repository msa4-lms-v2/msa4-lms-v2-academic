package com.msa4lmsv2academic.domain.grade.entity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public final class GradePointPolicy {

    private static final Map<String, BigDecimal> GRADE_POINTS = Map.of(
            "A+", new BigDecimal("4.5"),
            "A", new BigDecimal("4.0"),
            "B+", new BigDecimal("3.5"),
            "B", new BigDecimal("3.0"),
            "C+", new BigDecimal("2.5"),
            "C", new BigDecimal("2.0"),
            "D+", new BigDecimal("1.5"),
            "D", new BigDecimal("1.0"),
            "F", BigDecimal.ZERO
    );

    private GradePointPolicy() {
    }

    public static boolean isRecognized(String letterGrade) {
        return GRADE_POINTS.containsKey(letterGrade);
    }

    public static BigDecimal pointOf(String letterGrade) {
        return GRADE_POINTS.get(letterGrade);
    }

    public static Set<String> recognizedGrades() {
        return Set.copyOf(GRADE_POINTS.keySet());
    }
}
