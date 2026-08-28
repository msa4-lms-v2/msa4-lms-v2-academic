package com.msa4lmsv2academic.domain.grade.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RetakeGradeReflectionQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    public Optional<Enrollment> findEnrollmentForUpdate(Long enrollmentId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(enrollment)
                .join(enrollment.student, student).fetchJoin()
                .join(student.user).fetchJoin()
                .join(enrollment.lecture, lecture).fetchJoin()
                .join(lecture.course).fetchJoin()
                .join(lecture.semester, semester).fetchJoin()
                .where(enrollment.id.eq(enrollmentId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne());
    }

    public void lockStudent(Long studentId) {
        entityManager.find(Student.class, studentId, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<Enrollment> findActiveAttempts(Long studentId) {
        return queryFactory
                .selectFrom(enrollment)
                .join(enrollment.student, student).fetchJoin()
                .join(enrollment.lecture, lecture).fetchJoin()
                .join(lecture.course).fetchJoin()
                .join(lecture.semester, semester).fetchJoin()
                .where(
                        student.id.eq(studentId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)
                )
                .orderBy(
                        semester.academicYear.asc(),
                        semester.term.asc(),
                        enrollment.id.asc()
                )
                .fetch();
    }
}
