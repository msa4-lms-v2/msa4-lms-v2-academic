package com.msa4lmsv2academic.domain.student.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.organization.entity.QDepartment;
import com.msa4lmsv2academic.domain.professor.entity.QProfessor;
import com.msa4lmsv2academic.domain.student.entity.AcademicStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<ProfessorStudentScope> findProfessorScopeByUserId(Long userId) {
        Tuple result = jpaQueryFactory
                .select(professor.id, professor.department.id)
                .from(professor)
                .where(professor.user.id.eq(userId))
                .fetchOne();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(new ProfessorStudentScope(
                result.get(professor.id),
                result.get(professor.department.id)
        ));
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

    // 학적 이력 조회는 현재 관계만 제한하며, 자퇴·졸업·퇴학 학생도 제외하지 않는다.
    public JPQLQuery<Long> studentIdsInProfessorScope(ProfessorStudentScope scope) {
        return JPAExpressions.select(student.id)
                .from(student)
                .where(professorScopePredicate(scope));
    }

    public StudentSearchResult search(StudentSearchCondition condition) {
        BooleanBuilder predicates = searchPredicates(condition);
        QDepartment doubleMajor = new QDepartment("studentSearchDoubleMajor");
        QProfessor advisor = new QProfessor("studentSearchAdvisor");
        QUser advisorUser = new QUser("studentSearchAdvisorUser");

        List<Student> items = jpaQueryFactory
                .selectFrom(student)
                .join(student.user, user).fetchJoin()
                .join(student.department).fetchJoin()
                .leftJoin(student.doubleMajor, doubleMajor).fetchJoin()
                .leftJoin(student.advisor, advisor).fetchJoin()
                .leftJoin(advisor.user, advisorUser).fetchJoin()
                .where(predicates)
                .orderBy(primaryOrder(condition), student.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(student.count())
                .from(student)
                .join(student.user, user)
                .where(predicates)
                .fetchOne();

        return new StudentSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    private BooleanBuilder searchPredicates(StudentSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.keyword() != null) {
            predicates.and(student.user.name.containsIgnoreCase(condition.keyword()));
        }
        if (condition.departmentId() != null) {
            predicates.and(student.department.id.eq(condition.departmentId()));
        }
        if (condition.gradeLevel() != null) {
            predicates.and(student.gradeLevel.eq(condition.gradeLevel()));
        }
        if (condition.admissionYear() != null) {
            predicates.and(student.admissionYear.eq(condition.admissionYear()));
        }
        if (condition.academicStatus() != null) {
            predicates.and(student.academicStatus.eq(condition.academicStatus()));
        }
        if (condition.professorScope() != null) {
            predicates.and(professorScopePredicate(condition.professorScope()));
            predicates.and(student.academicStatus.in(AcademicStatus.ENROLLED, AcademicStatus.ON_LEAVE));
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

    private OrderSpecifier<?> primaryOrder(StudentSearchCondition condition) {
        return switch (condition.sortBy()) {
            case "gradeLevel" -> condition.descending() ? student.gradeLevel.desc() : student.gradeLevel.asc();
            case "admissionYear" -> condition.descending()
                    ? student.admissionYear.desc()
                    : student.admissionYear.asc();
            default -> condition.descending() ? student.user.name.desc() : student.user.name.asc();
        };
    }
}
