package com.msa4lmsv2academic.domain.graduation.entity;

import java.util.Set;

public final class GraduationCreditGradePolicy {

    private static final Set<String> PASSING_GRADES = Set.of(
            "A+", "A", "B+", "B", "C+", "C", "D+", "D"
    );

    private GraduationCreditGradePolicy() {
    }

    public static Set<String> passingGrades() {
        return PASSING_GRADES;
    }

    public static boolean isPassing(String letterGrade) {
        return letterGrade != null && PASSING_GRADES.contains(letterGrade);
    }
}
