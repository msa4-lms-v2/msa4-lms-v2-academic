package com.msa4lmsv2academic.domain.dismissal.repository;

import static com.msa4lmsv2academic.domain.dismissal.entity.QDismissalCandidate.dismissalCandidate;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.dismissal.entity.DismissalCandidate;
import com.msa4lmsv2academic.domain.dismissal.request.DismissalSearchRequestDTO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DismissalQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<DismissalCandidate> findDetail(Long id) {
        return Optional.ofNullable(queryFactory.selectFrom(dismissalCandidate)
                .join(dismissalCandidate.student, student).fetchJoin().join(student.user, user).fetchJoin()
                .where(dismissalCandidate.id.eq(id)).fetchOne());
    }

    public Page<DismissalCandidate> search(DismissalSearchRequestDTO filter, Pageable pageable) {
        // count에서도 사용자 조인이 생략되지 않도록 가시성 조건을 명시합니다.
        var where = new BooleanBuilder(user.deletedAt.isNull());
        if (filter.studentId() != null) where.and(student.id.eq(filter.studentId()));
        if (filter.departmentId() != null) where.and(student.department.id.eq(filter.departmentId()));
        if (filter.studentName() != null && !filter.studentName().isBlank()) where.and(user.name.contains(filter.studentName()));
        if (filter.reasonType() != null) where.and(dismissalCandidate.reasonType.eq(filter.reasonType()));
        if (filter.status() != null) where.and(dismissalCandidate.status.eq(filter.status()));
        var items = queryFactory.selectFrom(dismissalCandidate)
                .join(dismissalCandidate.student, student).fetchJoin().join(student.user, user).fetchJoin()
                .where(where)
                .orderBy(filter.ascending() ? dismissalCandidate.createdAt.asc() : dismissalCandidate.createdAt.desc(),
                        filter.ascending() ? dismissalCandidate.id.asc() : dismissalCandidate.id.desc())
                .offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        Long total = queryFactory.select(dismissalCandidate.count()).from(dismissalCandidate)
                .join(dismissalCandidate.student, student).join(student.user, user).where(where).fetchOne();
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }
}
