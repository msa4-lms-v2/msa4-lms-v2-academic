package com.msa4lmsv2academic.domain.notice.repository;

import static com.msa4lmsv2academic.domain.notice.entity.QNotice.notice;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoticeQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public NoticeSearchResult search(NoticeSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.keyword() != null) {
            predicates.and(
                    notice.title.containsIgnoreCase(condition.keyword())
                            .or(notice.content.containsIgnoreCase(condition.keyword()))
            );
        }
        if (condition.authorKeyword() != null) {
            predicates.and(notice.author.name.containsIgnoreCase(condition.authorKeyword()));
        }
        if (condition.category() != null) {
            predicates.and(notice.category.eq(condition.category()));
        }
        if (condition.targetRoles() != null && !condition.targetRoles().isEmpty()) {
            predicates.and(notice.targetRole.in(condition.targetRoles()));
        }
        if (condition.active() != null) {
            predicates.and(notice.active.eq(condition.active()));
        }
        if (condition.createdFrom() != null) {
            predicates.and(notice.createdAt.goe(condition.createdFrom().atStartOfDay()));
        }
        if (condition.createdTo() != null) {
            predicates.and(notice.createdAt.lt(condition.createdTo().plusDays(1).atStartOfDay()));
        }

        List<Notice> items = jpaQueryFactory
                .selectFrom(notice)
                .where(predicates)
                .orderBy(notice.createdAt.desc(), notice.id.desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(notice.count())
                .from(notice)
                .where(predicates)
                .fetchOne();

        return new NoticeSearchResult(items, totalCount == null ? 0 : totalCount);
    }
}
