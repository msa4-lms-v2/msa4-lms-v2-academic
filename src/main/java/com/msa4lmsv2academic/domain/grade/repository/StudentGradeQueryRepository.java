package com.msa4lmsv2academic.domain.grade.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.evaluation.entity.QLectureEvaluation.lectureEvaluation;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.entity.GradeStatus;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentGradeQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<StudentGradeQueryResult> findDisclosableGradesByStudentUserId(Long userId) {
        QUser studentUser = new QUser("studentGradeUser");

        return queryFactory
                .select(Projections.constructor(
                        StudentGradeQueryResult.class,
                        enrollment.id,
                        course.id,
                        lecture.id,
                        semester.academicYear,
                        semester.term,
                        course.code,
                        course.name,
                        course.credits,
                        enrollment.totalScore,
                        enrollment.letterGrade
                ))
                .from(enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.semester, semester)
                .where(
                        studentUser.id.eq(userId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        enrollment.gradeStatus.eq(GradeStatus.OPENED),
                        JPAExpressions
                                .selectOne()
                                .from(lectureEvaluation)
                                .where(lectureEvaluation.enrollment.id.eq(enrollment.id))
                                .exists()
                )
                .orderBy(
                        semester.academicYear.desc(),
                        semester.term.desc(),
                        course.name.asc(),
                        enrollment.id.desc()
                )
                .fetch();
    }

    public boolean existsStudentByUserId(Long userId) {
        QUser studentUser = new QUser("studentGradeExistenceUser");

        return queryFactory
                .selectOne()
                .from(student)
                .join(student.user, studentUser)
                .where(studentUser.id.eq(userId))
                .fetchFirst() != null;
    }
}
