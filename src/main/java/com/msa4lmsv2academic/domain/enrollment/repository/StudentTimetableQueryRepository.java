package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.lecture.entity.QLectureSchedule.lectureSchedule;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentTimetableQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public boolean existsStudentByUserId(Long userId) {
        QUser studentUser = new QUser("studentUser");
        return jpaQueryFactory
                .selectOne()
                .from(student)
                .join(student.user, studentUser)
                .where(studentUser.id.eq(userId))
                .fetchFirst() != null;
    }

    public List<StudentTimetableEntryQueryResult> findActiveTimetable(
            Long userId,
            Short academicYear,
            SemesterTerm term
    ) {
        QUser studentUser = new QUser("studentUser");
        QUser professorUser = new QUser("professorUser");
        List<Tuple> rows = jpaQueryFactory
                .select(
                        enrollment.id,
                        lecture.id,
                        course.id,
                        course.code,
                        course.name,
                        course.credits,
                        course.completionType,
                        professorUser.name,
                        lecture.sectionNo,
                        lecture.classroom
                )
                .from(enrollment)
                .join(enrollment.student, student)
                .join(student.user, studentUser)
                .join(enrollment.lecture, lecture)
                .join(lecture.course, course)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        studentUser.id.eq(userId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        semester.academicYear.eq(academicYear),
                        semester.term.eq(term)
                )
                .orderBy(
                        course.name.asc(),
                        lecture.sectionNo.asc(),
                        lecture.id.asc()
                )
                .fetch();

        List<Long> lectureIds = rows.stream().map(row -> row.get(lecture.id)).distinct().toList();
        Map<Long, List<StudentTimetableScheduleQueryResult>> schedulesByLectureId = findSchedules(lectureIds);
        return rows.stream()
                .map(row -> toQueryResult(row, professorUser, schedulesByLectureId))
                .toList();
    }

    private Map<Long, List<StudentTimetableScheduleQueryResult>> findSchedules(List<Long> lectureIds) {
        if (lectureIds.isEmpty()) {
            return Map.of();
        }
        List<StudentTimetableScheduleQueryResult> schedules = jpaQueryFactory
                .select(Projections.constructor(
                        StudentTimetableScheduleQueryResult.class,
                        lectureSchedule.lecture.id,
                        lectureSchedule.dayOfWeek,
                        lectureSchedule.startPeriod,
                        lectureSchedule.endPeriod
                ))
                .from(lectureSchedule)
                .where(lectureSchedule.lecture.id.in(lectureIds))
                .fetch().stream()
                .sorted(Comparator
                        .comparing(StudentTimetableScheduleQueryResult::lectureId)
                        .thenComparing(StudentTimetableScheduleQueryResult::dayOfWeek)
                        .thenComparing(StudentTimetableScheduleQueryResult::startPeriod)
                        .thenComparing(StudentTimetableScheduleQueryResult::endPeriod))
                .toList();

        Map<Long, List<StudentTimetableScheduleQueryResult>> result = new HashMap<>();
        for (StudentTimetableScheduleQueryResult schedule : schedules) {
            result.computeIfAbsent(schedule.lectureId(), ignored -> new ArrayList<>()).add(schedule);
        }
        return result;
    }

    private StudentTimetableEntryQueryResult toQueryResult(
            Tuple row,
            QUser professorUser,
            Map<Long, List<StudentTimetableScheduleQueryResult>> schedulesByLectureId
    ) {
        Long lectureId = row.get(lecture.id);
        return new StudentTimetableEntryQueryResult(
                row.get(enrollment.id),
                lectureId,
                row.get(course.id),
                row.get(course.code),
                row.get(course.name),
                row.get(course.credits),
                row.get(course.completionType),
                row.get(professorUser.name),
                row.get(lecture.sectionNo),
                row.get(lecture.classroom),
                List.copyOf(schedulesByLectureId.getOrDefault(lectureId, List.of()))
        );
    }
}
