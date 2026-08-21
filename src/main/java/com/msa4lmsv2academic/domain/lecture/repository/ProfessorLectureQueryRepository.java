package com.msa4lmsv2academic.domain.lecture.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.lecture.entity.QLectureSchedule.lectureSchedule;
import static com.msa4lmsv2academic.domain.organization.entity.QDepartment.department;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.jpa.JPAExpressions;
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
public class ProfessorLectureQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ProfessorLectureSearchResult searchByProfessorUserId(
            Long userId,
            Short academicYear,
            SemesterTerm term,
            LectureStatus status,
            long offset,
            int size
    ) {
        QUser professorUser = new QUser("professorUser");
        SubQueryExpression<Long> currentEnrollmentCount = JPAExpressions
                .select(enrollment.count())
                .from(enrollment)
                .where(
                        enrollment.lecture.eq(lecture),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)
                );

        List<Tuple> rows = jpaQueryFactory
                .select(
                        lecture.id,
                        course.id,
                        course.code,
                        course.name,
                        course.credits,
                        course.targetGrade,
                        course.completionType,
                        department.name,
                        professor.id,
                        professorUser.name,
                        semester.id,
                        semester.academicYear,
                        semester.term,
                        lecture.sectionNo,
                        lecture.classroom,
                        lecture.capacity,
                        lecture.status,
                        lecture.midtermRatio,
                        lecture.finalRatio,
                        lecture.assignmentRatio,
                        lecture.attendanceRatio,
                        lecture.syllabus,
                        currentEnrollmentCount
                )
                .from(lecture)
                .join(lecture.course, course)
                .join(course.department, department)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        professorUser.id.eq(userId),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        status == null ? null : lecture.status.eq(status)
                )
                .orderBy(
                        semester.academicYear.desc(),
                        semester.term.desc(),
                        course.name.asc(),
                        lecture.sectionNo.asc(),
                        lecture.id.asc()
                )
                .offset(offset)
                .limit(size)
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(lecture.count())
                .from(lecture)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        professorUser.id.eq(userId),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        status == null ? null : lecture.status.eq(status)
                )
                .fetchOne();

        List<Long> classIds = rows.stream()
                .map(row -> row.get(lecture.id))
                .toList();
        Map<Long, List<ProfessorLectureScheduleQueryResult>> schedulesByClassId = findSchedules(classIds);

        List<ProfessorLectureQueryResult> items = rows.stream()
                .map(row -> toQueryResult(row, professorUser, currentEnrollmentCount, schedulesByClassId))
                .toList();
        return new ProfessorLectureSearchResult(items, totalCount == null ? 0L : totalCount);
    }

    public boolean existsProfessorByUserId(Long userId) {
        QUser professorUser = new QUser("professorUser");

        return jpaQueryFactory
                .selectOne()
                .from(professor)
                .join(professor.user, professorUser)
                .where(professorUser.id.eq(userId))
                .fetchFirst() != null;
    }

    private Map<Long, List<ProfessorLectureScheduleQueryResult>> findSchedules(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }

        List<ProfessorLectureScheduleQueryResult> schedules = jpaQueryFactory
                .select(Projections.constructor(
                        ProfessorLectureScheduleQueryResult.class,
                        lectureSchedule.lecture.id,
                        lectureSchedule.dayOfWeek,
                        lectureSchedule.startPeriod,
                        lectureSchedule.endPeriod
                ))
                .from(lectureSchedule)
                .where(lectureSchedule.lecture.id.in(classIds))
                .fetch();
        schedules = schedules.stream()
                .sorted(Comparator
                        .comparing(ProfessorLectureScheduleQueryResult::classId)
                        .thenComparing(ProfessorLectureScheduleQueryResult::dayOfWeek)
                        .thenComparing(ProfessorLectureScheduleQueryResult::startPeriod)
                        .thenComparing(ProfessorLectureScheduleQueryResult::endPeriod))
                .toList();

        Map<Long, List<ProfessorLectureScheduleQueryResult>> schedulesByClassId = new HashMap<>();
        for (ProfessorLectureScheduleQueryResult schedule : schedules) {
            schedulesByClassId.computeIfAbsent(schedule.classId(), ignored -> new ArrayList<>())
                    .add(schedule);
        }
        return schedulesByClassId;
    }

    private ProfessorLectureQueryResult toQueryResult(
            Tuple row,
            QUser professorUser,
            SubQueryExpression<Long> currentEnrollmentCount,
            Map<Long, List<ProfessorLectureScheduleQueryResult>> schedulesByClassId
    ) {
        Long classId = row.get(lecture.id);
        return new ProfessorLectureQueryResult(
                classId,
                row.get(course.id),
                row.get(course.code),
                row.get(course.name),
                row.get(course.credits),
                row.get(course.targetGrade),
                row.get(course.completionType),
                row.get(department.name),
                row.get(professor.id),
                row.get(professorUser.name),
                row.get(semester.id),
                row.get(semester.academicYear),
                row.get(semester.term),
                row.get(lecture.sectionNo),
                row.get(lecture.classroom),
                row.get(lecture.capacity),
                row.get(lecture.status),
                row.get(lecture.midtermRatio),
                row.get(lecture.finalRatio),
                row.get(lecture.assignmentRatio),
                row.get(lecture.attendanceRatio),
                row.get(lecture.syllabus),
                row.get(currentEnrollmentCount),
                List.copyOf(schedulesByClassId.getOrDefault(classId, List.of()))
        );
    }
}
