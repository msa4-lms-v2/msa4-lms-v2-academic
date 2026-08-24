package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QCoursePrerequisite.coursePrerequisite;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.course.entity.QCourse;
import com.msa4lmsv2academic.domain.enrollment.entity.CoursePrerequisite;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PrerequisiteRetakeRuleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public PrerequisiteRetakeRuleSearchResult search(PrerequisiteRetakeRuleSearchCondition condition) {
        QCourse targetCourse = new QCourse("prerequisiteRuleTargetCourse");
        QCourse requiredCourse = new QCourse("prerequisiteRuleRequiredCourse");
        BooleanBuilder predicates = searchPredicates(condition, targetCourse, requiredCourse);

        List<CoursePrerequisite> items = jpaQueryFactory
                .selectFrom(coursePrerequisite)
                .join(coursePrerequisite.course, targetCourse).fetchJoin()
                .join(targetCourse.department).fetchJoin()
                .join(coursePrerequisite.prerequisiteCourse, requiredCourse).fetchJoin()
                .join(requiredCourse.department).fetchJoin()
                .where(predicates)
                .orderBy(primaryOrder(condition, targetCourse, requiredCourse), coursePrerequisite.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(coursePrerequisite.count())
                .from(coursePrerequisite)
                .join(coursePrerequisite.course, targetCourse)
                .join(coursePrerequisite.prerequisiteCourse, requiredCourse)
                .where(predicates)
                .fetchOne();
        return new PrerequisiteRetakeRuleSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public Optional<CoursePrerequisite> findByIdWithCourses(Long ruleId) {
        QCourse targetCourse = new QCourse("prerequisiteRuleDetailTargetCourse");
        QCourse requiredCourse = new QCourse("prerequisiteRuleDetailRequiredCourse");
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(coursePrerequisite)
                .join(coursePrerequisite.course, targetCourse).fetchJoin()
                .join(targetCourse.department).fetchJoin()
                .join(coursePrerequisite.prerequisiteCourse, requiredCourse).fetchJoin()
                .join(requiredCourse.department).fetchJoin()
                .where(coursePrerequisite.id.eq(ruleId))
                .fetchOne());
    }

    public List<CoursePrerequisite> findActiveRulesByCourseId(Long courseId) {
        QCourse targetCourse = new QCourse("activeRuleTargetCourse");
        QCourse requiredCourse = new QCourse("activeRuleRequiredCourse");
        return jpaQueryFactory
                .selectFrom(coursePrerequisite)
                .join(coursePrerequisite.course, targetCourse).fetchJoin()
                .join(targetCourse.department).fetchJoin()
                .join(coursePrerequisite.prerequisiteCourse, requiredCourse).fetchJoin()
                .join(requiredCourse.department).fetchJoin()
                .where(
                        targetCourse.id.eq(courseId),
                        coursePrerequisite.active.isTrue()
                )
                .orderBy(requiredCourse.code.asc(), coursePrerequisite.id.asc())
                .fetch();
    }

    public List<CoursePrerequisiteEdge> findActiveEdges() {
        return jpaQueryFactory
                .select(Projections.constructor(
                        CoursePrerequisiteEdge.class,
                        coursePrerequisite.id,
                        coursePrerequisite.course.id,
                        coursePrerequisite.prerequisiteCourse.id
                ))
                .from(coursePrerequisite)
                .where(coursePrerequisite.active.isTrue())
                .fetch();
    }

    public Optional<Course> findCourseById(Long courseId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(course)
                .join(course.department).fetchJoin()
                .where(course.id.eq(courseId))
                .fetchOne());
    }

    public Optional<Long> findStudentIdByUserId(Long userId) {
        return Optional.ofNullable(jpaQueryFactory
                .select(student.id)
                .from(student)
                .where(student.user.id.eq(userId))
                .fetchOne());
    }

    public boolean existsStudentById(Long studentId) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(student.id.eq(studentId))
                .fetchFirst() != null;
    }

    public List<CourseGradeAttemptQueryResult> findGradeAttempts(Long studentId, List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        QCourse attemptedCourse = new QCourse("prerequisiteRuleAttemptedCourse");
        return jpaQueryFactory
                .select(Projections.constructor(
                        CourseGradeAttemptQueryResult.class,
                        attemptedCourse.id,
                        enrollment.id,
                        enrollment.status,
                        enrollment.gradeStatus,
                        enrollment.letterGrade,
                        semester.academicYear,
                        semester.term,
                        semester.current
                ))
                .from(enrollment)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, attemptedCourse)
                .join(lecture.semester, semester)
                .where(
                        enrollment.student.id.eq(studentId),
                        attemptedCourse.id.in(courseIds)
                )
                .orderBy(
                        attemptedCourse.id.asc(),
                        semester.academicYear.desc(),
                        semester.term.desc(),
                        enrollment.id.desc()
                )
                .fetch();
    }

    private BooleanBuilder searchPredicates(
            PrerequisiteRetakeRuleSearchCondition condition,
            QCourse targetCourse,
            QCourse requiredCourse
    ) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.keyword() != null) {
            predicates.and(
                    targetCourse.code.containsIgnoreCase(condition.keyword())
                            .or(targetCourse.name.containsIgnoreCase(condition.keyword()))
                            .or(requiredCourse.code.containsIgnoreCase(condition.keyword()))
                            .or(requiredCourse.name.containsIgnoreCase(condition.keyword()))
            );
        }
        if (condition.courseId() != null) {
            predicates.and(targetCourse.id.eq(condition.courseId()));
        }
        if (condition.active() != null) {
            predicates.and(coursePrerequisite.active.eq(condition.active()));
        }
        return predicates;
    }

    private OrderSpecifier<?> primaryOrder(
            PrerequisiteRetakeRuleSearchCondition condition,
            QCourse targetCourse,
            QCourse requiredCourse
    ) {
        return switch (condition.sortBy()) {
            case "prerequisiteCourseCode" -> condition.descending()
                    ? requiredCourse.code.desc()
                    : requiredCourse.code.asc();
            case "createdAt" -> condition.descending()
                    ? coursePrerequisite.createdAt.desc()
                    : coursePrerequisite.createdAt.asc();
            case "updatedAt" -> condition.descending()
                    ? coursePrerequisite.updatedAt.desc()
                    : coursePrerequisite.updatedAt.asc();
            default -> condition.descending() ? targetCourse.code.desc() : targetCourse.code.asc();
        };
    }
}
