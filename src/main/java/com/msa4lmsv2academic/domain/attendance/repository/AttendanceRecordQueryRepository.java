package com.msa4lmsv2academic.domain.attendance.repository;

import static com.msa4lmsv2academic.domain.attendance.entity.QAttendance.attendance;
import static com.msa4lmsv2academic.domain.attendance.entity.QAttendanceSession.attendanceSession;
import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.attendance.entity.Attendance;
import com.msa4lmsv2academic.domain.attendance.entity.AttendanceStatus;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.msa4lmsv2academic.domain.user.entity.UserRole;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AttendanceRecordQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public AttendanceRecordSearchResult search(
            Long userId,
            UserRole role,
            Long classId,
            Long enrollmentId,
            LocalDate fromDate,
            LocalDate toDate,
            AttendanceStatus status,
            long offset,
            int size
    ) {
        QUser studentUser = new QUser("attendanceStudentUser");
        QUser professorUser = new QUser("attendanceProfessorUser");

        List<AttendanceRecordQueryResult> items = jpaQueryFactory
                .select(Projections.constructor(
                        AttendanceRecordQueryResult.class,
                        attendance.id,
                        enrollment.id,
                        student.id,
                        studentUser.name,
                        lecture.id,
                        course.code,
                        course.name,
                        lecture.sectionNo,
                        professorUser.name,
                        semester.academicYear,
                        semester.term,
                        attendanceSession.id,
                        attendance.lectureDate,
                        attendance.period,
                        attendance.status,
                        attendance.remarks,
                        attendance.checkInTime,
                        attendance.modified,
                        attendance.updatedAt
                ))
                .from(attendance)
                .join(attendance.enrollment, enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .leftJoin(attendance.session, attendanceSession)
                .where(
                        ownership(userId, role, studentUser, professorUser),
                        classId == null ? null : lecture.id.eq(classId),
                        enrollmentId == null ? null : enrollment.id.eq(enrollmentId),
                        fromDate == null ? null : attendance.lectureDate.goe(fromDate),
                        toDate == null ? null : attendance.lectureDate.loe(toDate),
                        status == null ? null : attendance.status.eq(status)
                )
                .orderBy(attendance.lectureDate.desc(), attendance.period.asc(), attendance.id.desc())
                .offset(offset)
                .limit(size)
                .fetch();

        long totalCount = count(
                userId, role, classId, enrollmentId, fromDate, toDate, status,
                studentUser, professorUser
        );
        return new AttendanceRecordSearchResult(items, totalCount);
    }

    public Optional<Attendance> findByIdForUpdate(Long attendanceId) {
        QUser studentUser = new QUser("updateAttendanceStudentUser");
        QUser professorUser = new QUser("updateAttendanceProfessorUser");

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(attendance)
                .join(attendance.enrollment, enrollment).fetchJoin()
                .join(enrollment.student, student).fetchJoin()
                .join(student.user, studentUser).fetchJoin()
                .join(enrollment.lecture, lecture).fetchJoin()
                .join(lecture.course, course).fetchJoin()
                .join(lecture.professor, professor).fetchJoin()
                .join(professor.user, professorUser).fetchJoin()
                .join(lecture.semester, semester).fetchJoin()
                .leftJoin(attendance.session, attendanceSession).fetchJoin()
                .where(attendance.id.eq(attendanceId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne());
    }

    private long count(
            Long userId,
            UserRole role,
            Long classId,
            Long enrollmentId,
            LocalDate fromDate,
            LocalDate toDate,
            AttendanceStatus status,
            QUser studentUser,
            QUser professorUser
    ) {
        JPAQuery<Long> query = jpaQueryFactory
                .select(attendance.count())
                .from(attendance)
                .join(attendance.enrollment, enrollment);

        if (role == UserRole.STUDENT) {
            query.join(enrollment.student, student)
                    .join(student.user, studentUser);
        }
        if (role == UserRole.PROFESSOR || classId != null) {
            query.join(enrollment.lecture, lecture);
        }
        if (role == UserRole.PROFESSOR) {
            query.join(lecture.professor, professor)
                    .join(professor.user, professorUser);
        }

        Long totalCount = query
                .where(
                        ownership(userId, role, studentUser, professorUser),
                        classId == null ? null : lecture.id.eq(classId),
                        enrollmentId == null ? null : enrollment.id.eq(enrollmentId),
                        fromDate == null ? null : attendance.lectureDate.goe(fromDate),
                        toDate == null ? null : attendance.lectureDate.loe(toDate),
                        status == null ? null : attendance.status.eq(status)
                )
                .fetchOne();
        return totalCount == null ? 0L : totalCount;
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
