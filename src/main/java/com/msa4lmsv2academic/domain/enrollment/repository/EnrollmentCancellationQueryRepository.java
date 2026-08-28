package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentCancellationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Enrollment> findOwnedEnrollmentForUpdate(Long enrollmentId, Long userId) {
        QUser studentUser = new QUser("studentUser");
        return Optional.ofNullable(queryFactory
                .selectFrom(enrollment)
                .join(enrollment.student, student).fetchJoin()
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture).fetchJoin()
                .join(lecture.semester, semester).fetchJoin()
                .where(
                        enrollment.id.eq(enrollmentId),
                        studentUser.id.eq(userId)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne());
    }
}
