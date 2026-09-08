package com.msa4lmsv2academic.domain.evaluation.repository;

import static com.msa4lmsv2academic.domain.course.entity.QCourse.course;
import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.evaluation.entity.QLectureEvaluation.lectureEvaluation;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.evaluation.entity.LectureEvaluation;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfessorLectureEvaluationQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ProfessorLectureEvaluationSearchResult searchLecturesByProfessorUserId(
            Long professorUserId,
            Long lectureId,
            Short academicYear,
            SemesterTerm term,
            Boolean current,
            long offset,
            int size
    ) {
        QUser professorUser = new QUser("evaluationProfessorUser");
        SubQueryExpression<Long> activeEnrollmentCount = JPAExpressions
                .select(enrollment.count())
                .from(enrollment)
                .where(
                        enrollment.lecture.eq(lecture),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)
                );

        List<Tuple> rows = jpaQueryFactory
                .select(
                        lecture.id,
                        course.code,
                        course.name,
                        lecture.sectionNo,
                        semester.academicYear,
                        semester.term,
                        activeEnrollmentCount
                )
                .from(lecture)
                .join(lecture.course, course)
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .join(lecture.semester, semester)
                .where(
                        professorUser.id.eq(professorUserId),
                        lectureId == null ? null : lecture.id.eq(lectureId),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        current == null ? null : semester.current.eq(current)
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
                        professorUser.id.eq(professorUserId),
                        lectureId == null ? null : lecture.id.eq(lectureId),
                        academicYear == null ? null : semester.academicYear.eq(academicYear),
                        term == null ? null : semester.term.eq(term),
                        current == null ? null : semester.current.eq(current)
                )
                .fetchOne();

        List<ProfessorLectureEvaluationQueryResult> items = rows.stream()
                .map(row -> new ProfessorLectureEvaluationQueryResult(
                        row.get(lecture.id),
                        row.get(course.code),
                        row.get(course.name),
                        row.get(lecture.sectionNo),
                        row.get(semester.academicYear),
                        row.get(semester.term),
                        resolvedCount(row.get(activeEnrollmentCount))
                ))
                .toList();
        return new ProfessorLectureEvaluationSearchResult(
                items,
                totalCount == null ? 0L : totalCount
        );
    }

    public List<LectureEvaluation> findEvaluationResponses(
            Long professorUserId,
            List<Long> lectureIds
    ) {
        if (lectureIds.isEmpty()) {
            return List.of();
        }
        QUser professorUser = new QUser("evaluationResponseProfessorUser");

        return jpaQueryFactory
                .selectFrom(lectureEvaluation)
                .join(lectureEvaluation.enrollment, enrollment).fetchJoin()
                .join(enrollment.lecture, lecture).fetchJoin()
                .join(lecture.professor, professor)
                .join(professor.user, professorUser)
                .where(
                        professorUser.id.eq(professorUserId),
                        lecture.id.in(lectureIds),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE)
                )
                .orderBy(lecture.id.asc(), lectureEvaluation.id.asc())
                .fetch();
    }

    public boolean existsProfessorByUserId(Long userId) {
        QUser professorUser = new QUser("evaluationExistingProfessorUser");

        return jpaQueryFactory
                .selectOne()
                .from(professor)
                .join(professor.user, professorUser)
                .where(professorUser.id.eq(userId))
                .fetchFirst() != null;
    }

    private long resolvedCount(Long count) {
        return count == null ? 0L : count;
    }
}
