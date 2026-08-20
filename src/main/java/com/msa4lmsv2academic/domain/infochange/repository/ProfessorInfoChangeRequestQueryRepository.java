package com.msa4lmsv2academic.domain.infochange.repository;

import static com.msa4lmsv2academic.domain.infochange.entity.QProfessorInfoChangeRequest.professorInfoChangeRequest;

import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfessorInfoChangeRequestQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ProfessorInfoChangeRequestSearchResult search(InfoChangeRequestSearchCondition condition) {
        BooleanBuilder predicates = predicates(condition);
        OrderSpecifier<?> createdOrder = condition.sortDirection() == Sort.Direction.ASC
                ? professorInfoChangeRequest.createdAt.asc()
                : professorInfoChangeRequest.createdAt.desc();
        OrderSpecifier<?> idOrder = condition.sortDirection() == Sort.Direction.ASC
                ? professorInfoChangeRequest.id.asc()
                : professorInfoChangeRequest.id.desc();

        List<ProfessorInfoChangeRequest> items = jpaQueryFactory
                .selectFrom(professorInfoChangeRequest)
                .join(professorInfoChangeRequest.professor).fetchJoin()
                .join(professorInfoChangeRequest.professor.user).fetchJoin()
                .join(professorInfoChangeRequest.professor.department).fetchJoin()
                .leftJoin(professorInfoChangeRequest.reviewedBy).fetchJoin()
                .where(predicates)
                .orderBy(createdOrder, idOrder)
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(professorInfoChangeRequest.count())
                .from(professorInfoChangeRequest)
                .join(professorInfoChangeRequest.professor)
                .join(professorInfoChangeRequest.professor.user)
                .where(predicates)
                .fetchOne();

        return new ProfessorInfoChangeRequestSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    private BooleanBuilder predicates(InfoChangeRequestSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.requesterUserId() != null) {
            predicates.and(professorInfoChangeRequest.professor.user.id.eq(condition.requesterUserId()));
        }
        if (condition.status() != null) {
            predicates.and(professorInfoChangeRequest.status.eq(condition.status()));
        }
        if (condition.departmentId() != null) {
            predicates.and(professorInfoChangeRequest.professor.department.id.eq(condition.departmentId()));
        }
        if (condition.keyword() != null) {
            predicates.and(professorInfoChangeRequest.professor.user.name.containsIgnoreCase(condition.keyword()));
        }
        return predicates;
    }
}
