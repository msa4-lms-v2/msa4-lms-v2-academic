package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    @EntityGraph(attributePaths = {"course", "course.department", "prerequisiteCourse",
            "prerequisiteCourse.department"})
    Optional<CoursePrerequisite> findByCourseIdAndPrerequisiteCourseId(
            Long courseId,
            Long prerequisiteCourseId
    );

    boolean existsByCourseIdAndPrerequisiteCourseIdAndIdNot(
            Long courseId,
            Long prerequisiteCourseId,
            Long ruleId
    );
}
