package com.msa4lmsv2academic.domain.notice.repository;

import static com.msa4lmsv2academic.domain.notice.entity.QNotice.notice;

import com.msa4lmsv2academic.domain.notice.entity.Notice;
import com.msa4lmsv2academic.domain.notice.entity.NoticeTargetRole;
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
        if (condition.targetRoles() != null && !condition.targetRoles().isEmpty()) {
            predicates.and(notice.targetRole.in(condition.targetRoles()));
        }
        if (condition.active() != null) {
            predicates.and(notice.active.eq(condition.active()));
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

    public boolean existsActiveDuplicate(String title, String content, NoticeTargetRole targetRole,
                                         Long excludedNoticeId) {
        BooleanBuilder predicates = new BooleanBuilder()
                .and(notice.active.isTrue())
                .and(notice.title.eq(title))
                .and(notice.targetRole.eq(targetRole));

        predicates.and(content == null ? notice.content.isNull() : notice.content.eq(content));
        if (excludedNoticeId != null) {
            predicates.and(notice.id.ne(excludedNoticeId));
        }

        return jpaQueryFactory
                .selectOne()
                .from(notice)
                .where(predicates)
                .fetchFirst() != null;
    }
}
