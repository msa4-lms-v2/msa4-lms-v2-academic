package com.msa4lmsv2academic.domain.lecture.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.lecture.entity.QLectureSchedule.lectureSchedule;
import static com.msa4lmsv2academic.domain.organization.entity.QCollege.college;
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
public class AvailableLectureQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ProfessorLectureSearchResult search(
            Short academicYear,
            SemesterTerm term,
            Long collegeId,
            Long departmentId,
            Byte targetGrade,
            String courseName,
            String professorName,
            LectureStatus status,
            long offset,
            int size
    ) {
        QUser professorUser = new QUser("availableLectureProfessorUser");
        SubQueryExpression<Long> currentEnrollmentCount = JPAExpressions
                .select(enrollment.count())
                .from(enrollment)
                .where(enrollment.lecture.eq(lecture), enrollment.status.eq(EnrollmentStatus.ACTIVE));

        List<Tuple> rows = jpaQueryFactory
                .select(
                        lecture.id, course.id, course.code, course.name, course.credits,
                        course.targetGrade, course.completionType, department.name,
                        professor.id, professorUser.name, semester.id, semester.academicYear,
                        semester.term, lecture.sectionNo, lecture.classroom, lecture.capacity,
                        lecture.status, lecture.midtermRatio, lecture.finalRatio,
                        lecture.assignmentRatio, lecture.attendanceRatio, lecture.syllabus,
                        currentEnrollmentCount
                )
                .from(lecture)
                .join(lecture.course, course)
                .join(course.department, department)
                .leftJoin(department.college, college)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        collegeId == null ? null : college.id.eq(collegeId),
                        departmentId == null ? null : department.id.eq(departmentId),
                        targetGrade == null ? null : course.targetGrade.eq(targetGrade),
                        hasText(courseName) ? course.name.containsIgnoreCase(courseName.trim()) : null,
                        hasText(professorName) ? professorUser.name.containsIgnoreCase(professorName.trim()) : null,
                        status == null ? null : lecture.status.eq(status)
                )
                .orderBy(course.name.asc(), lecture.sectionNo.asc(), lecture.id.asc())
                .offset(offset)
                .limit(size)
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(lecture.count())
                .from(lecture)
                .join(lecture.course, course)
                .join(course.department, department)
                .leftJoin(department.college, college)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        collegeId == null ? null : college.id.eq(collegeId),
                        departmentId == null ? null : department.id.eq(departmentId),
                        targetGrade == null ? null : course.targetGrade.eq(targetGrade),
                        hasText(courseName) ? course.name.containsIgnoreCase(courseName.trim()) : null,
                        hasText(professorName) ? professorUser.name.containsIgnoreCase(professorName.trim()) : null,
                        status == null ? null : lecture.status.eq(status)
                )
                .fetchOne();

        List<Long> classIds = rows.stream().map(row -> row.get(lecture.id)).toList();
        Map<Long, List<ProfessorLectureScheduleQueryResult>> schedulesByClassId = findSchedules(classIds);
        List<ProfessorLectureQueryResult> items = rows.stream()
                .map(row -> toQueryResult(row, professorUser, currentEnrollmentCount, schedulesByClassId))
                .toList();
        return new ProfessorLectureSearchResult(items, totalCount == null ? 0L : totalCount);
    }

    private Map<Long, List<ProfessorLectureScheduleQueryResult>> findSchedules(List<Long> classIds) {
        if (classIds.isEmpty()) return Map.of();
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
                .fetch().stream()
                .sorted(Comparator
                        .comparing(ProfessorLectureScheduleQueryResult::classId)
                        .thenComparing(ProfessorLectureScheduleQueryResult::dayOfWeek)
                        .thenComparing(ProfessorLectureScheduleQueryResult::startPeriod)
                        .thenComparing(ProfessorLectureScheduleQueryResult::endPeriod))
                .toList();
        Map<Long, List<ProfessorLectureScheduleQueryResult>> result = new HashMap<>();
        schedules.forEach(schedule -> result.computeIfAbsent(schedule.classId(), ignored -> new ArrayList<>()).add(schedule));
        return result;
    }

    private ProfessorLectureQueryResult toQueryResult(
            Tuple row,
            QUser professorUser,
            SubQueryExpression<Long> currentEnrollmentCount,
            Map<Long, List<ProfessorLectureScheduleQueryResult>> schedulesByClassId
    ) {
        Long classId = row.get(lecture.id);
        return new ProfessorLectureQueryResult(
                classId, row.get(course.id), row.get(course.code), row.get(course.name),
                row.get(course.credits), row.get(course.targetGrade), row.get(course.completionType),
                row.get(department.name), row.get(professor.id), row.get(professorUser.name),
                row.get(semester.id), row.get(semester.academicYear), row.get(semester.term),
                row.get(lecture.sectionNo), row.get(lecture.classroom), row.get(lecture.capacity),
                row.get(lecture.status), row.get(lecture.midtermRatio), row.get(lecture.finalRatio),
                row.get(lecture.assignmentRatio), row.get(lecture.attendanceRatio), row.get(lecture.syllabus),
                row.get(currentEnrollmentCount), List.copyOf(schedulesByClassId.getOrDefault(classId, List.of()))
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
