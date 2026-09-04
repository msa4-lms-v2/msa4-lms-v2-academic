package com.msa4lmsv2academic.domain.attendance.repository;

import static com.msa4lmsv2academic.domain.attendance.entity.QExcuseRequest.excuseRequest;
import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExcuseRequestQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ExcuseRequestSearchResult search(
            Long userId,
            UserRole role,
            ExcuseRequestStatus status,
            long offset,
            int size
    ) {
        QUser studentUser = new QUser("studentUser");
        QUser professorUser = new QUser("professorUser");
        Predicate ownership = ownership(userId, role, studentUser, professorUser);

        List<ExcuseRequestQueryResult> items = jpaQueryFactory
                .select(Projections.constructor(
                        ExcuseRequestQueryResult.class,
                        excuseRequest.id,
                        enrollment.id,
                        student.id,
                        studentUser.id,
                        studentUser.name,
                        lecture.id,
                        course.id,
                        course.code,
                        course.name,
                        lecture.sectionNo,
                        professor.id,
                        professorUser.id,
                        professorUser.name,
                        excuseRequest.lectureDate,
                        excuseRequest.period,
                        excuseRequest.reason,
                        excuseRequest.status,
                        excuseRequest.rejectReason,
                        excuseRequest.attachmentOriginalName,
                        excuseRequest.attachmentContentType,
                        excuseRequest.attachmentSize,
                        excuseRequest.createdAt,
                        excuseRequest.updatedAt
                ))
                .from(excuseRequest)
                .join(excuseRequest.enrollment, enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .where(
                        ownership,
                        status == null ? null : excuseRequest.status.eq(status)
                )
                .orderBy(excuseRequest.createdAt.desc(), excuseRequest.id.desc())
                .offset(offset)
                .limit(size)
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(excuseRequest.count())
                .from(excuseRequest)
                .join(excuseRequest.enrollment, enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .where(
                        ownership,
                        status == null ? null : excuseRequest.status.eq(status)
                )
                .fetchOne();

        return new ExcuseRequestSearchResult(items, totalCount == null ? 0L : totalCount);
    }

    private Predicate ownership(
            Long userId,
            UserRole role,
            QUser studentUser,
            QUser professorUser
    ) {
        return switch (role) {
            case STUDENT -> studentUser.id.eq(userId);
            case PROFESSOR -> professorUser.id.eq(userId);
            case ADMIN -> null;
        };
    }
}
