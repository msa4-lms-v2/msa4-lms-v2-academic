package com.msa4lmsv2academic.domain.enrollment.repository;

public record CoursePrerequisiteEdge(
        Long ruleId,
        Long courseId,
        Long prerequisiteCourseId
) {
}
