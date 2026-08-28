package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollmentCart.enrollmentCart;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.lecture.entity.QLectureSchedule.lectureSchedule;
import static com.msa4lmsv2academic.domain.organization.entity.QDepartment.department;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCart;
import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentCartQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Optional<Student> findStudentByUserId(Long userId) {
        QUser studentUser = new QUser("studentUser");
        return Optional.ofNullable(jpaQueryFactory.selectFrom(student)
                .join(student.user, studentUser)
                .where(studentUser.id.eq(userId))
                .fetchOne());
    }

    public Optional<Lecture> findLecture(Long lectureId) {
        return Optional.ofNullable(jpaQueryFactory.selectFrom(lecture)
                .join(lecture.semester, semester).fetchJoin()
                .where(lecture.id.eq(lectureId))
                .fetchOne());
    }

    public boolean existsByStudentAndLecture(Long studentId, Long lectureId) {
        return jpaQueryFactory.selectOne()
                .from(enrollmentCart)
                .where(
                        enrollmentCart.student.id.eq(studentId),
                        enrollmentCart.lecture.id.eq(lectureId)
                )
                .fetchFirst() != null;
    }

    public Optional<EnrollmentCart> findOwnedItemForUpdate(Long cartItemId, Long studentId) {
        return Optional.ofNullable(jpaQueryFactory.selectFrom(enrollmentCart)
                .join(enrollmentCart.lecture, lecture).fetchJoin()
                .join(lecture.semester, semester).fetchJoin()
                .where(
                        enrollmentCart.id.eq(cartItemId),
                        enrollmentCart.student.id.eq(studentId)
                )
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne());
    }

    public List<EnrollmentCartItemQueryResult> findByStudentUserId(
            Long userId,
            Short academicYear,
            SemesterTerm term
    ) {
        QUser studentUser = new QUser("studentUser");
        QUser professorUser = new QUser("professorUser");
        List<Tuple> rows = jpaQueryFactory
                .select(
                        enrollmentCart.id,
                        enrollmentCart.createdAt,
                        lecture.id,
                        course.id,
                        course.code,
                        course.name,
                        course.credits,
                        course.targetGrade,
                        course.completionType,
                        department.name,
                        professorUser.name,
                        semester.id,
                        semester.academicYear,
                        semester.term,
                        lecture.sectionNo,
                        lecture.classroom,
                        lecture.capacity,
                        lecture.status
                )
                .from(enrollmentCart)
                .join(enrollmentCart.student, student)
                .join(student.user, studentUser)
                .join(enrollmentCart.lecture, lecture)
                .join(lecture.course, course)
                .join(course.department, department)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        studentUser.id.eq(userId),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term)
                )
                .orderBy(
                        semester.academicYear.desc(),
                        semester.term.desc(),
                        course.name.asc(),
                        lecture.sectionNo.asc(),
                        enrollmentCart.id.asc()
                )
                .fetch();

        List<Long> lectureIds = rows.stream().map(row -> row.get(lecture.id)).toList();
        Map<Long, List<EnrollmentCartScheduleQueryResult>> schedulesByLectureId = findSchedules(lectureIds);
        return rows.stream()
                .map(row -> toQueryResult(row, professorUser, schedulesByLectureId))
                .toList();
    }

    private Map<Long, List<EnrollmentCartScheduleQueryResult>> findSchedules(List<Long> lectureIds) {
        if (lectureIds.isEmpty()) {
            return Map.of();
        }
        List<EnrollmentCartScheduleQueryResult> schedules = jpaQueryFactory
                .select(Projections.constructor(
                        EnrollmentCartScheduleQueryResult.class,
                        lectureSchedule.lecture.id,
                        lectureSchedule.dayOfWeek,
                        lectureSchedule.startPeriod,
                        lectureSchedule.endPeriod
                ))
                .from(lectureSchedule)
                .where(lectureSchedule.lecture.id.in(lectureIds))
                .fetch().stream()
                .sorted(Comparator
                        .comparing(EnrollmentCartScheduleQueryResult::lectureId)
                        .thenComparing(EnrollmentCartScheduleQueryResult::dayOfWeek)
                        .thenComparing(EnrollmentCartScheduleQueryResult::startPeriod)
                        .thenComparing(EnrollmentCartScheduleQueryResult::endPeriod))
                .toList();

        Map<Long, List<EnrollmentCartScheduleQueryResult>> result = new HashMap<>();
        for (EnrollmentCartScheduleQueryResult schedule : schedules) {
            result.computeIfAbsent(schedule.lectureId(), ignored -> new ArrayList<>()).add(schedule);
        }
        return result;
    }

    private EnrollmentCartItemQueryResult toQueryResult(
            Tuple row,
            QUser professorUser,
            Map<Long, List<EnrollmentCartScheduleQueryResult>> schedulesByLectureId
    ) {
        Long lectureId = row.get(lecture.id);
        return new EnrollmentCartItemQueryResult(
                row.get(enrollmentCart.id),
                row.get(enrollmentCart.createdAt),
                lectureId,
                row.get(course.id),
                row.get(course.code),
                row.get(course.name),
                row.get(course.credits),
                row.get(course.targetGrade),
                row.get(course.completionType),
                row.get(department.name),
                row.get(professorUser.name),
                row.get(semester.id),
                row.get(semester.academicYear),
                row.get(semester.term),
                row.get(lecture.sectionNo),
                row.get(lecture.classroom),
                row.get(lecture.capacity),
                row.get(lecture.status),
                List.copyOf(schedulesByLectureId.getOrDefault(lectureId, List.of()))
        );
    }
}
