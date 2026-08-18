package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.organization.entity.QDepartment.department;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentClassQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<StudentClassQueryResult> findActiveClassesByStudentUserId(
            Long userId,
            Short academicYear,
            SemesterTerm term
    ) {
        QUser studentUser = new QUser("studentUser");
        QUser professorUser = new QUser("professorUser");

        return jpaQueryFactory
                .select(Projections.constructor(
                        StudentClassQueryResult.class,
                        lecture.id,
                        course.id,
                        course.code,
                        course.name,
                        course.credits,
                        course.targetGrade,
                        course.completionType,
                        department.name,
                        professorUser.name,
                        semester.academicYear,
                        semester.term,
                        lecture.sectionNo,
                        lecture.classroom,
                        lecture.capacity,
                        lecture.status
                ))
                .distinct()
                .from(enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(course.department, department)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        studentUser.id.eq(userId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term)
                )
                .orderBy(
                        semester.academicYear.desc(),
                        semester.term.desc(),
                        course.name.asc(),
                        lecture.sectionNo.asc(),
                        lecture.id.asc()
                )
                .fetch();
    }

    public boolean existsStudentByUserId(Long userId) {
        QUser studentUser = new QUser("studentUser");

        return jpaQueryFactory
                .selectOne()
                .from(student)
                .join(student.user, studentUser)
                .where(studentUser.id.eq(userId))
                .fetchFirst() != null;
    }
}
