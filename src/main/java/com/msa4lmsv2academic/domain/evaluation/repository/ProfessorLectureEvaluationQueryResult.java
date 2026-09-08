package com.msa4lmsv2academic.domain.evaluation.repository;

import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;

public record ProfessorLectureEvaluationQueryResult(
        Long lectureId,
        String courseCode,
        String courseName,
        String sectionNo,
        Short academicYear,
        SemesterTerm term,
        long activeEnrollmentCount
) {
}
