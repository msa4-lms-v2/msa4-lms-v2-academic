package com.msa4lmsv2academic.domain.doublemajor.repository;

import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.transfer.entity.QAcademicChangeRequest.academicChangeRequest;
import static com.msa4lmsv2academic.domain.transfer.entity.QAcademicChangeRequestPeriod.academicChangeRequestPeriod;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.doublemajor.request.*;
import com.msa4lmsv2academic.domain.organization.entity.QDepartment;
import com.msa4lmsv2academic.domain.organization.entity.QMajor;
import com.msa4lmsv2academic.domain.transfer.entity.*;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DoubleMajorQueryRepository {
    private final JPAQueryFactory queryFactory;

    private static final QDepartment SOURCE_DEPARTMENT = new QDepartment("doubleMajorSourceDepartment");
    private static final QDepartment TARGET_DEPARTMENT = new QDepartment("doubleMajorTargetDepartment");
    private static final QMajor SOURCE_MAJOR = new QMajor("doubleMajorSourceMajor");
    private static final QMajor TARGET_MAJOR = new QMajor("doubleMajorTargetMajor");
    private static final QAcademicChangeRequestPeriod REQUEST_PERIOD =
            new QAcademicChangeRequestPeriod("doubleMajorRequestPeriod");
    private static final QUser PROCESSED_BY = new QUser("doubleMajorProcessedBy");
    private static final QUser CANCELLED_BY = new QUser("doubleMajorCancelledBy");

    public Optional<AcademicChangeRequest> findDetail(Long id) {
        return Optional.ofNullable(baseDetailQuery().where(academicChangeRequest.id.eq(id),
                academicChangeRequest.requestType.eq(AcademicChangeRequestType.DOUBLE_MAJOR)).fetchOne());
    }

    public Page<AcademicChangeRequest> search(DoubleMajorSearchRequestDTO filter, Long ownerUserId,
                                              Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder(
                academicChangeRequest.requestType.eq(AcademicChangeRequestType.DOUBLE_MAJOR));
        if (ownerUserId != null) where.and(academicChangeRequest.student.user.id.eq(ownerUserId));
        if (filter.studentId() != null) where.and(academicChangeRequest.student.id.eq(filter.studentId()));
        if (filter.status() != null) where.and(academicChangeRequest.status.eq(filter.status()));
        if (filter.requestPeriodId() != null) where.and(academicChangeRequest.requestPeriod.id.eq(filter.requestPeriodId()));
        if (filter.targetDepartmentId() != null) where.and(academicChangeRequest.targetDepartment.id.eq(filter.targetDepartmentId()));
        if (filter.targetMajorId() != null) where.and(academicChangeRequest.targetMajor.id.eq(filter.targetMajorId()));
        if (filter.normalizedKeyword() != null) {
            String keyword = filter.normalizedKeyword();
            where.and(academicChangeRequest.student.user.name.containsIgnoreCase(keyword)
                    .or(academicChangeRequest.sourceDepartment.name.containsIgnoreCase(keyword))
                    .or(academicChangeRequest.sourceMajor.name.containsIgnoreCase(keyword))
                    .or(academicChangeRequest.targetDepartment.name.containsIgnoreCase(keyword))
                    .or(academicChangeRequest.targetMajor.name.containsIgnoreCase(keyword)));
        }
        var items = baseDetailQuery().where(where)
                .orderBy(filter.ascending() ? academicChangeRequest.createdAt.asc() : academicChangeRequest.createdAt.desc(),
                        filter.ascending() ? academicChangeRequest.id.asc() : academicChangeRequest.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(academicChangeRequest.count()).from(academicChangeRequest)
                .where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    public Page<AcademicChangeRequestPeriod> searchPeriods(DoubleMajorPeriodSearchRequestDTO filter,
                                                            boolean studentOnly, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder(
                academicChangeRequestPeriod.requestType.eq(AcademicChangeRequestType.DOUBLE_MAJOR));
        if (filter.semesterId() != null) where.and(academicChangeRequestPeriod.semester.id.eq(filter.semesterId()));
        if (studentOnly) where.and(academicChangeRequestPeriod.active.isTrue());
        if (filter.active() != null) where.and(academicChangeRequestPeriod.active.eq(filter.active()));
        var items = queryFactory.selectFrom(academicChangeRequestPeriod)
                .join(academicChangeRequestPeriod.semester, semester).fetchJoin().where(where)
                .orderBy(semester.academicYear.desc(), semester.term.desc(), academicChangeRequestPeriod.id.desc())
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
                .leftJoin(academicChangeRequest.sourceMajor, SOURCE_MAJOR).fetchJoin()
                .join(academicChangeRequest.targetDepartment, TARGET_DEPARTMENT).fetchJoin()
                .join(academicChangeRequest.targetMajor, TARGET_MAJOR).fetchJoin()
                .join(academicChangeRequest.requestPeriod, REQUEST_PERIOD).fetchJoin()
                .join(REQUEST_PERIOD.semester, semester).fetchJoin()
                .leftJoin(academicChangeRequest.processedBy, PROCESSED_BY).fetchJoin()
                .leftJoin(academicChangeRequest.cancelledBy, CANCELLED_BY).fetchJoin();
    }
}
