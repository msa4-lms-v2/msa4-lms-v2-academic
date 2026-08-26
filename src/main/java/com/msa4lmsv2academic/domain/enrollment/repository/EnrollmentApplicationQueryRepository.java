package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.lecture.entity.QLectureSchedule;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentApplicationQueryRepository {
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public Optional<Student> findStudentByUserIdForUpdate(Long userId) {
        return Optional.ofNullable(queryFactory.selectFrom(student)
                .where(student.user.id.eq(userId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).fetchOne());
    }

    public Optional<Lecture> findLectureForUpdate(Long lectureId) {
        return Optional.ofNullable(entityManager.find(Lecture.class, lectureId, LockModeType.PESSIMISTIC_WRITE));
    }

    public boolean existsActiveEnrollment(Long studentId, Long lectureId) {
        return queryFactory.selectOne().from(enrollment)
                .where(enrollment.student.id.eq(studentId), enrollment.lecture.id.eq(lectureId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)).fetchFirst() != null;
    }

    public long countActiveEnrollments(Long lectureId) {
        Long count = queryFactory.select(enrollment.count()).from(enrollment)
                .where(enrollment.lecture.id.eq(lectureId), enrollment.status.eq(EnrollmentStatus.ACTIVE)).fetchOne();
        return count == null ? 0 : count;
    }

    public boolean hasScheduleConflict(Long studentId, Lecture target) {
        QLectureSchedule requested = new QLectureSchedule("requestedSchedule");
        QLectureSchedule existing = new QLectureSchedule("existingSchedule");
        return queryFactory.selectOne().from(enrollment)
                .join(existing).on(existing.lecture.id.eq(enrollment.lecture.id))
                .join(requested).on(requested.lecture.id.eq(target.getId()))
                .where(enrollment.student.id.eq(studentId), enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        enrollment.lecture.semester.id.eq(target.getSemester().getId()),
                        existing.dayOfWeek.eq(requested.dayOfWeek),
                        existing.startPeriod.loe(requested.endPeriod),
                        existing.endPeriod.goe(requested.startPeriod)).fetchFirst() != null;
    }
}
