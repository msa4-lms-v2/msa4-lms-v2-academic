package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentCreditQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public long sumActiveCredits(Long studentId, Long semesterId) {
        Long credits = jpaQueryFactory
                .select(course.credits.longValue().sum().coalesce(0L))
                .from(enrollment)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .where(
                        enrollment.student.id.eq(studentId),
                        lecture.semester.id.eq(semesterId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)
                )
                .fetchOne();
        return credits == null ? 0 : credits;
    }
}
