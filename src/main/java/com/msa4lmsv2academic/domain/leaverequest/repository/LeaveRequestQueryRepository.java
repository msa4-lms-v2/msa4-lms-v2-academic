package com.msa4lmsv2academic.domain.leaverequest.repository;

import static com.msa4lmsv2academic.domain.leaverequest.entity.QLeaveRequest.leaveRequest;
import static com.msa4lmsv2academic.domain.leaverequest.entity.QLeaveRequestPeriod.leaveRequestPeriod;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;
import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;
import static com.msa4lmsv2academic.domain.withdrawal.entity.QAcademicStatusHistory.academicStatusHistory;

import com.msa4lmsv2academic.domain.leaverequest.entity.*;
import com.msa4lmsv2academic.domain.leaverequest.request.*;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LeaveRequestQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<Student> findStudentByUserId(Long userId) {
        return Optional.ofNullable(queryFactory.selectFrom(student).join(student.user, user).fetchJoin()
                .where(user.id.eq(userId)).fetchOne());
    }

    public Optional<Semester> findSemesterForUpdate(Long id) {
        return Optional.ofNullable(queryFactory.selectFrom(semester).where(semester.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).fetchOne());
    }

    public List<Semester> findCurrentSemesters() {
        return queryFactory.selectFrom(semester).where(semester.current.isTrue()).limit(2).fetch();
    }

    public Optional<AcademicStatusHistory> findLatestHistory(Long studentId) {
        return Optional.ofNullable(queryFactory.selectFrom(academicStatusHistory)
                .where(academicStatusHistory.student.id.eq(studentId))
                .orderBy(academicStatusHistory.createdAt.desc(), academicStatusHistory.id.desc()).fetchFirst());
    }

    public Optional<LeaveRequestPeriod> findPeriod(short year, byte term, LeaveRequestType type, boolean lock) {
        var query = queryFactory.selectFrom(leaveRequestPeriod)
                .join(leaveRequestPeriod.semester, semester).fetchJoin()
                .where(semester.academicYear.eq(year),
                        semester.term.eq(term == 1 ? SemesterTerm.FIRST : SemesterTerm.SECOND),
                        leaveRequestPeriod.requestType.eq(type));
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(query.fetchOne());
    }

    public Optional<LeaveRequest> findDetail(Long id) {
        return Optional.ofNullable(queryFactory.selectFrom(leaveRequest)
                .join(leaveRequest.student, student).fetchJoin().join(student.user, user).fetchJoin()
                .where(leaveRequest.id.eq(id)).fetchOne());
    }

    public Page<LeaveRequest> search(LeaveRequestSearchRequestDTO filter, Long ownerUserId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        if (ownerUserId != null) where.and(leaveRequest.student.user.id.eq(ownerUserId));
        if (filter.studentId() != null) where.and(leaveRequest.student.id.eq(filter.studentId()));
        if (filter.requestType() != null) where.and(leaveRequest.requestType.eq(filter.requestType()));
        if (filter.status() != null) where.and(leaveRequest.status.eq(filter.status()));
        if (filter.targetYear() != null) where.and(leaveRequest.targetYear.eq(filter.targetYear()));
        if (filter.targetSemester() != null) where.and(leaveRequest.targetSemester.eq(filter.targetSemester()));
        boolean ascending = filter.resolvedSort() == LeaveRequestSort.CREATED_AT_ASC;
        var items = queryFactory.selectFrom(leaveRequest)
                .join(leaveRequest.student, student).fetchJoin().join(student.user, user).fetchJoin()
                .where(where).orderBy(ascending ? leaveRequest.createdAt.asc() : leaveRequest.createdAt.desc(),
                        ascending ? leaveRequest.id.asc() : leaveRequest.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(leaveRequest.count()).from(leaveRequest).where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    public Page<LeaveRequestPeriod> searchPeriods(LeavePeriodSearchRequestDTO filter, boolean studentOnly, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        if (filter.semesterId() != null) where.and(leaveRequestPeriod.semester.id.eq(filter.semesterId()));
        if (filter.requestType() != null) where.and(leaveRequestPeriod.requestType.eq(filter.requestType()));
        if (studentOnly) where.and(leaveRequestPeriod.active.isTrue());
        if (filter.active() != null) where.and(leaveRequestPeriod.active.eq(filter.active()));
        var items = queryFactory.selectFrom(leaveRequestPeriod)
                .join(leaveRequestPeriod.semester, semester).fetchJoin()
                .where(where).orderBy(semester.academicYear.desc(), semester.term.desc(),
                        leaveRequestPeriod.requestType.asc(), leaveRequestPeriod.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(leaveRequestPeriod.count()).from(leaveRequestPeriod).where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }
}
