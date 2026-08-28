package com.msa4lmsv2academic.domain.graduation.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.graduation.entity.QGraduationRequirement.graduationRequirement;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.organization.entity.QDepartment.department;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.graduation.entity.GraduationCreditGradePolicy;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.repository.ProfessorStudentScope;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GraduationCreditQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<GraduationCreditDiagnosisQueryResult> findCreditDiagnosisByStudentId(Long studentId) {
        Tuple requirement = jpaQueryFactory
                .select(
                        graduationRequirement.requiredMajorCredits,
                        graduationRequirement.requiredGeneralCredits,
                        graduationRequirement.requiredTotalCredits
                )
                .from(student)
                .join(graduationRequirement)
                .on(
                        graduationRequirement.department.eq(student.department),
                        graduationRequirement.admissionYear.eq(student.admissionYear)
                )
                .where(student.id.eq(studentId))
                .fetchOne();

        if (requirement == null) {
            return Optional.empty();
        }

        EarnedCreditTotals earned = findEarnedCreditsByStudentIds(List.of(studentId))
                .getOrDefault(studentId, EarnedCreditTotals.empty());
        return Optional.of(new GraduationCreditDiagnosisQueryResult(
                requiredValue(requirement, graduationRequirement.requiredMajorCredits),
                requiredValue(requirement, graduationRequirement.requiredGeneralCredits),
                requiredValue(requirement, graduationRequirement.requiredTotalCredits),
                earned.major(),
                earned.general(),
                earned.required(),
                earned.elective(),
                earned.total()
        ));
    }

    public List<CreditDiagnosisCandidateRow> findDiagnosisCandidates(
            CreditDiagnosisSearchCondition condition,
            boolean paged
    ) {
        QUser studentUser = new QUser("creditDiagnosisStudentUser");
        BooleanBuilder predicates = candidatePredicates(condition, studentUser);
        JPAQuery<CreditDiagnosisCandidateRow> query = jpaQueryFactory
                .select(Projections.constructor(
                        CreditDiagnosisCandidateRow.class,
                        student.id,
                        studentUser.name,
                        department.id,
                        department.name,
                        student.admissionYear,
                        student.academicStatus,
                        graduationRequirement.id,
                        graduationRequirement.requiredMajorCredits,
                        graduationRequirement.requiredGeneralCredits,
                        graduationRequirement.requiredTotalCredits
                ))
                .from(student)
                .join(student.user, studentUser)
                .join(student.department, department)
                .leftJoin(graduationRequirement)
                .on(
                        graduationRequirement.department.eq(student.department),
                        graduationRequirement.admissionYear.eq(student.admissionYear)
                )
                .where(predicates)
                .orderBy(primaryOrder(condition, studentUser), student.id.asc());

        if (paged) {
            query.offset(condition.offset()).limit(condition.limit());
        }
        return query.fetch();
    }

    public long countDiagnosisCandidates(CreditDiagnosisSearchCondition condition) {
        QUser studentUser = new QUser("creditDiagnosisCountStudentUser");
        Long count = jpaQueryFactory
                .select(student.count())
                .from(student)
                .join(student.user, studentUser)
                .where(candidatePredicates(condition, studentUser))
                .fetchOne();
        return count == null ? 0 : count;
    }

    public Map<Long, EarnedCreditTotals> findEarnedCreditsByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> openedAttempts = jpaQueryFactory
                .select(
                        student.id,
                        course.id,
                        course.credits,
                        course.completionType,
                        semester.academicYear,
                        semester.term,
                        enrollment.id,
                        enrollment.letterGrade
                )
                .from(enrollment)
                .join(enrollment.student, student)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.semester, semester)
                .where(
                        student.id.in(studentIds),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        enrollment.gradeStatus.eq(GradeStatus.OPENED)
                )
                .fetch();

        Map<StudentCourseKey, OpenedAttempt> latestByStudentCourse = new HashMap<>();
        for (Tuple row : openedAttempts) {
            OpenedAttempt attempt = openedAttempt(row);
            if (attempt.studentId() == null || attempt.courseId() == null) {
                continue;
            }
            latestByStudentCourse.merge(
                    new StudentCourseKey(attempt.studentId(), attempt.courseId()),
                    attempt,
                    (left, right) -> compareAttempt(left, right) >= 0 ? left : right
            );
        }

        Map<Long, MutableCreditTotals> totals = new HashMap<>();
        for (OpenedAttempt attempt : latestByStudentCourse.values()) {
            if (attempt.credits() == null || attempt.completionType() == null
                    || !GraduationCreditGradePolicy.isPassing(attempt.letterGrade())) {
                continue;
            }
            totals.computeIfAbsent(attempt.studentId(), ignored -> new MutableCreditTotals())
                    .add(attempt.completionType(), attempt.credits());
        }

        Map<Long, EarnedCreditTotals> result = new LinkedHashMap<>();
        totals.forEach((studentId, total) -> result.put(studentId, total.toImmutable()));
        return Map.copyOf(result);
    }

    public List<GraduationCreditRecordQueryResult> findCreditRecordsByStudentId(Long studentId) {
        return jpaQueryFactory
                .select(Projections.constructor(
                        GraduationCreditRecordQueryResult.class,
                        enrollment.id,
                        course.id,
                        course.code,
                        course.name,
                        course.credits,
                        course.completionType,
                        semester.academicYear,
                        semester.term,
                        enrollment.status,
                        enrollment.gradeStatus,
                        enrollment.letterGrade
                ))
                .from(enrollment)
                .join(enrollment.student, student)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.semester, semester)
                .where(student.id.eq(studentId))
                .fetch();
    }

    public int sumTotalCreditsByStudentId(Long studentId) {
        return findEarnedCreditsByStudentIds(List.of(studentId))
                .getOrDefault(studentId, EarnedCreditTotals.empty())
                .total();
    }

    public boolean isStudentOwnedByUser(Long studentId, Long userId) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(student.id.eq(studentId), student.user.id.eq(userId))
                .fetchFirst() != null;
    }

    public boolean existsStudentById(Long studentId) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(student.id.eq(studentId))
                .fetchFirst() != null;
    }

    public boolean isStudentInProfessorScope(Long studentId, ProfessorStudentScope scope) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(
                        student.id.eq(studentId),
                        student.academicStatus.in(AcademicStatus.ENROLLED, AcademicStatus.ON_LEAVE),
                        professorScopePredicate(scope)
                )
                .fetchFirst() != null;
    }

    public boolean existsStudentInDepartmentAndAdmissionYear(Long departmentId, short admissionYear) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(
                        student.department.id.eq(departmentId),
                        student.admissionYear.eq(admissionYear)
                )
                .fetchFirst() != null;
    }

    private BooleanBuilder candidatePredicates(CreditDiagnosisSearchCondition condition, QUser studentUser) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.keyword() != null) {
            predicates.and(studentUser.name.containsIgnoreCase(condition.keyword()));
        }
        if (condition.departmentId() != null) {
            predicates.and(student.department.id.eq(condition.departmentId()));
        }
        if (condition.admissionYear() != null) {
            predicates.and(student.admissionYear.eq(condition.admissionYear()));
        }
        if (condition.academicStatus() != null) {
            predicates.and(student.academicStatus.eq(condition.academicStatus()));
        }
        if (condition.studentUserId() != null) {
            predicates.and(studentUser.id.eq(condition.studentUserId()));
        }
        if (condition.professorScope() != null) {
            predicates.and(student.academicStatus.in(AcademicStatus.ENROLLED, AcademicStatus.ON_LEAVE));
            predicates.and(professorScopePredicate(condition.professorScope()));
        }
        return predicates;
    }

    private BooleanExpression professorScopePredicate(ProfessorStudentScope scope) {
        BooleanExpression currentLectureStudent = JPAExpressions
                .selectOne()
                .from(enrollment)
                .join(enrollment.lecture, lecture)
                .join(lecture.semester, semester)
                .where(
                        enrollment.student.eq(student),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        lecture.professor.id.eq(scope.professorId()),
                        semester.current.isTrue()
                )
                .exists();
        return student.advisor.id.eq(scope.professorId())
                .or(student.department.id.eq(scope.departmentId()))
                .or(currentLectureStudent);
    }

    private OrderSpecifier<?> primaryOrder(CreditDiagnosisSearchCondition condition, QUser studentUser) {
        return switch (condition.sortBy()) {
            case "departmentName" -> condition.descending() ? department.name.desc() : department.name.asc();
            case "admissionYear" -> condition.descending()
                    ? student.admissionYear.desc()
                    : student.admissionYear.asc();
            default -> condition.descending() ? studentUser.name.desc() : studentUser.name.asc();
        };
    }

    private int requiredValue(Tuple requirement, com.querydsl.core.types.Expression<Integer> expression) {
        Integer value = requirement.get(expression);
        return value == null ? 0 : value;
    }

    private OpenedAttempt openedAttempt(Tuple row) {
        return new OpenedAttempt(
                row.get(student.id),
                row.get(course.id),
                row.get(course.credits),
                row.get(course.completionType),
                row.get(semester.academicYear),
                row.get(semester.term),
                row.get(enrollment.id),
                row.get(enrollment.letterGrade)
        );
    }

    private int compareAttempt(OpenedAttempt left, OpenedAttempt right) {
        int yearComparison = Short.compare(left.academicYear(), right.academicYear());
        if (yearComparison != 0) {
            return yearComparison;
        }
        int termComparison = Integer.compare(termOrder(left.term()), termOrder(right.term()));
        if (termComparison != 0) {
            return termComparison;
        }
        return Long.compare(left.enrollmentId(), right.enrollmentId());
    }

    private int termOrder(SemesterTerm term) {
        return term == SemesterTerm.SECOND ? 2 : 1;
    }

    private record StudentCourseKey(Long studentId, Long courseId) {
    }

    private record OpenedAttempt(
            Long studentId,
            Long courseId,
            Byte credits,
            CompletionType completionType,
            short academicYear,
            SemesterTerm term,
            Long enrollmentId,
            String letterGrade
    ) {
    }

    private static final class MutableCreditTotals {

        private int major;
        private int general;
        private int required;
        private int elective;

        private void add(CompletionType completionType, int credits) {
            switch (completionType) {
                case MAJOR_REQUIRED -> {
                    major += credits;
                    required += credits;
                }
                case MAJOR_ELECTIVE -> {
                    major += credits;
                    elective += credits;
                }
                case GENERAL_REQUIRED -> {
                    general += credits;
                    required += credits;
                }
                case GENERAL_ELECTIVE -> {
                    general += credits;
                    elective += credits;
                }
            }
        }

        private EarnedCreditTotals toImmutable() {
            return new EarnedCreditTotals(major, general, required, elective, major + general);
        }
    }
}
