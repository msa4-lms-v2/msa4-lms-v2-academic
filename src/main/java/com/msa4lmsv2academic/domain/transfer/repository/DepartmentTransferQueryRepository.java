package com.msa4lmsv2academic.domain.transfer.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollment.enrollment;
import static com.msa4lmsv2academic.domain.lecture.entity.QLecture.lecture;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.transfer.entity.QAcademicChangeRequest.academicChangeRequest;
import static com.msa4lmsv2academic.domain.transfer.entity.QAcademicChangeRequestPeriod.academicChangeRequestPeriod;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.organization.entity.QDepartment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.professor.entity.QProfessor;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferPeriodSearchRequestDTO;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferSearchRequestDTO;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DepartmentTransferQueryRepository {
    private final JPAQueryFactory queryFactory;

    private static final QDepartment SOURCE_DEPARTMENT = new QDepartment("sourceDepartment");
    private static final QDepartment TARGET_DEPARTMENT = new QDepartment("targetDepartment");
    private static final QUser PROCESSED_BY = new QUser("processedBy");
    private static final QUser ADVISOR_REVIEWED_BY = new QUser("advisorReviewedBy");
    private static final QProfessor ADVISOR = new QProfessor("transferAdvisor");
    private static final QUser ADVISOR_USER = new QUser("transferAdvisorUser");
    private static final QUser CANCELLED_BY = new QUser("cancelledBy");

    public Optional<AcademicChangeRequest> findDetail(Long id) {
        return Optional.ofNullable(baseDetailQuery()
                .where(academicChangeRequest.id.eq(id),
                        academicChangeRequest.requestType.eq(AcademicChangeRequestType.TRANSFER_DEPARTMENT))
                .fetchOne());
    }

    public Page<AcademicChangeRequest> search(DepartmentTransferSearchRequestDTO filter, Long ownerUserId,
                                              Long advisorUserId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder(
                academicChangeRequest.requestType.eq(AcademicChangeRequestType.TRANSFER_DEPARTMENT));
        if (ownerUserId != null) where.and(academicChangeRequest.student.user.id.eq(ownerUserId));
        if (advisorUserId != null) {
            where.and(academicChangeRequest.student.advisor.id.in(
                    JPAExpressions.select(ADVISOR.id).from(ADVISOR)
                            .join(ADVISOR.user, ADVISOR_USER)
                            .where(ADVISOR_USER.id.eq(advisorUserId))));
        }
        if (filter.studentId() != null) where.and(academicChangeRequest.student.id.eq(filter.studentId()));
        if (filter.status() != null) where.and(academicChangeRequest.status.eq(filter.status()));
        if (filter.targetSemesterId() != null) where.and(academicChangeRequest.targetSemester.id.eq(filter.targetSemesterId()));
        if (filter.targetDepartmentId() != null) where.and(academicChangeRequest.targetDepartment.id.eq(filter.targetDepartmentId()));
        if (filter.normalizedKeyword() != null) {
            String keyword = filter.normalizedKeyword();
            where.and(academicChangeRequest.student.user.name.containsIgnoreCase(keyword)
                    .or(academicChangeRequest.sourceDepartment.name.containsIgnoreCase(keyword))
                    .or(academicChangeRequest.targetDepartment.name.containsIgnoreCase(keyword)));
        }
        var items = baseDetailQuery().where(where)
                .orderBy(filter.ascending() ? academicChangeRequest.createdAt.asc() : academicChangeRequest.createdAt.desc(),
                        filter.ascending() ? academicChangeRequest.id.asc() : academicChangeRequest.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(academicChangeRequest.count()).from(academicChangeRequest)
                .where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    public boolean hasCompletedSemesterEnrollment(Long studentId, LocalDate applicationDate) {
        Integer found = queryFactory.selectOne()
                .from(enrollment)
                .join(enrollment.lecture, lecture)
                .join(lecture.semester, semester)
                .where(enrollment.student.id.eq(studentId),
                        enrollment.status.eq(EnrollmentStatus.ACTIVE),
                        semester.endDate.lt(applicationDate))
                .fetchFirst();
        return found != null;
    }

    public Page<AcademicChangeRequestPeriod> searchPeriods(DepartmentTransferPeriodSearchRequestDTO filter,
                                                            boolean studentOnly, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder(
                academicChangeRequestPeriod.requestType.eq(AcademicChangeRequestType.TRANSFER_DEPARTMENT));
        if (filter.semesterId() != null) where.and(academicChangeRequestPeriod.semester.id.eq(filter.semesterId()));
        if (studentOnly) where.and(academicChangeRequestPeriod.active.isTrue());
        if (filter.active() != null) where.and(academicChangeRequestPeriod.active.eq(filter.active()));
        var items = queryFactory.selectFrom(academicChangeRequestPeriod)
                .join(academicChangeRequestPeriod.semester, semester).fetchJoin()
                .where(where).orderBy(semester.academicYear.desc(), semester.term.desc(),
                        academicChangeRequestPeriod.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(academicChangeRequestPeriod.count()).from(academicChangeRequestPeriod)
                .where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    private com.querydsl.jpa.impl.JPAQuery<AcademicChangeRequest> baseDetailQuery() {
        return queryFactory.selectFrom(academicChangeRequest)
                .join(academicChangeRequest.student, student).fetchJoin()
                .join(student.user, user).fetchJoin()
                .join(academicChangeRequest.sourceDepartment, SOURCE_DEPARTMENT).fetchJoin()
                .join(academicChangeRequest.targetDepartment, TARGET_DEPARTMENT).fetchJoin()
                .join(academicChangeRequest.targetSemester, semester).fetchJoin()
                .leftJoin(academicChangeRequest.advisorReviewedBy, ADVISOR_REVIEWED_BY).fetchJoin()
                .leftJoin(academicChangeRequest.processedBy, PROCESSED_BY).fetchJoin()
                .leftJoin(academicChangeRequest.cancelledBy, CANCELLED_BY).fetchJoin();
    }
}
