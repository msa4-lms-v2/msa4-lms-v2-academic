package com.msa4lmsv2academic.domain.graduation.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.graduation.entity.QGraduationRequirement.graduationRequirement;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.course.entity.CompletionType;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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
                .orderBy(graduationRequirement.id.desc())
                .fetchFirst();

        if (requirement == null) {
            return Optional.empty();
        }

        CreditTotals earned = sumCredits(findCompletedCourses(studentId));
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

    // 졸업 요건(graduationRequirement) 존재 여부와 무관하게 취득 학점 합계만 필요한 조회(예: 학적조회 화면)용.
    public int sumTotalCreditsByStudentId(Long studentId) {
        return sumCredits(findCompletedCourses(studentId)).total();
    }

    private List<Tuple> findCompletedCourses(Long studentId) {
        return jpaQueryFactory
                .select(course.id, course.credits, course.completionType)
                .from(enrollment)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .where(
                        enrollment.student.id.eq(studentId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        enrollment.gradeStatus.eq(GradeStatus.OPENED),
                        enrollment.letterGrade.isNotNull(),
                        enrollment.letterGrade.ne("F")
                )
                .groupBy(course.id, course.credits, course.completionType)
                .fetch();
    }

    public boolean isStudentOwnedByUser(Long studentId, Long userId) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .where(student.id.eq(studentId), student.user.id.eq(userId))
                .fetchFirst() != null;
    }

    public boolean isStudentAdvisedByUser(Long studentId, Long userId) {
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .join(student.advisor, professor)
                .join(professor.user, user)
                .where(student.id.eq(studentId), user.id.eq(userId))
                .fetchFirst() != null;
    }

    private CreditTotals sumCredits(List<Tuple> completedCourses) {
        int major = 0;
        int general = 0;
        int required = 0;
        int elective = 0;

        for (Tuple completedCourse : completedCourses) {
            Byte credits = completedCourse.get(course.credits);
            CompletionType completionType = completedCourse.get(course.completionType);
            if (credits == null || completionType == null) {
                continue;
            }

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
        return new CreditTotals(major, general, required, elective, major + general);
    }

    private int requiredValue(Tuple requirement, com.querydsl.core.types.Expression<Integer> expression) {
        Integer value = requirement.get(expression);
        return value == null ? 0 : value;
    }

    private record CreditTotals(int major, int general, int required, int elective, int total) {
    }
}
