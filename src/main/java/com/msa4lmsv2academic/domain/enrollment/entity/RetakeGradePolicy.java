package com.msa4lmsv2academic.domain.enrollment.entity;

import java.util.Map;
import java.util.Set;

public final class RetakeGradePolicy {

    private static final Set<String> RETAKE_ALLOWED_GRADES = Set.of("C+", "C", "D+", "D");
    private static final Set<String> RETAKE_BLOCKING_GRADES = Set.of("A+", "A", "B+", "B");
    private static final Map<String, Integer> GRADE_RANK = Map.of(
            "A+", 8,
            "A", 7,
            "B+", 6,
            "B", 5,
            "C+", 4,
            "C", 3,
            "D+", 2,
            "D", 1,
            "F", 0
    );

    private RetakeGradePolicy() {
    }

    public static boolean isAllowedForRetake(String grade) {
        return RETAKE_ALLOWED_GRADES.contains(grade);
    }

    public static boolean blocksRetake(String grade) {
        return RETAKE_BLOCKING_GRADES.contains(grade);
    }

    public static boolean completesPrerequisite(String grade) {
        return isAllowedForRetake(grade) || blocksRetake(grade);
    }

    public static boolean isRecognized(String grade) {
        return GRADE_RANK.containsKey(grade);
    }

    public static int rank(String grade) {
        return GRADE_RANK.getOrDefault(grade, -1);
    }
}
