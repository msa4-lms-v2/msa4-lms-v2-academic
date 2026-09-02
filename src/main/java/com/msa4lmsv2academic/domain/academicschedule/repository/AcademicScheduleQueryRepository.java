package com.msa4lmsv2academic.domain.academicschedule.repository;

import static com.msa4lmsv2academic.domain.academicschedule.entity.QAcademicSchedule.academicSchedule;

import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicSchedule;
import com.msa4lmsv2academic.domain.academicschedule.entity.AcademicScheduleTargetRole;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AcademicScheduleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public AcademicScheduleSearchResult search(AcademicScheduleSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.keyword() != null) {
            predicates.and(
                    academicSchedule.title.containsIgnoreCase(condition.keyword())
                            .or(academicSchedule.content.containsIgnoreCase(condition.keyword()))
            );
        }
        if (condition.from() != null) {
            predicates.and(
                    academicSchedule.endDate.goe(condition.from())
                            .or(academicSchedule.endDate.isNull()
                                    .and(academicSchedule.startDate.goe(condition.from())))
            );
        }
        if (condition.to() != null) {
            predicates.and(academicSchedule.startDate.loe(condition.to()));
        }
        if (condition.targetRoles() != null && !condition.targetRoles().isEmpty()) {
            predicates.and(academicSchedule.targetRole.in(condition.targetRoles()));
        }
        if (condition.active() != null) {
            predicates.and(academicSchedule.active.eq(condition.active()));
        }

        List<AcademicSchedule> items = jpaQueryFactory
                .selectFrom(academicSchedule)
                .where(predicates)
                .orderBy(academicSchedule.startDate.asc(), academicSchedule.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(academicSchedule.count())
                .from(academicSchedule)
                .where(predicates)
                .fetchOne();

        return new AcademicScheduleSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public boolean existsDuplicate(Long excludedId, String title, String content, LocalDate startDate,
                                   LocalDate endDate, AcademicScheduleTargetRole targetRole) {
        BooleanBuilder predicates = new BooleanBuilder()
                .and(academicSchedule.active.isTrue())
                .and(academicSchedule.title.eq(title))
                .and(nullableTextEquals(content))
                .and(academicSchedule.startDate.eq(startDate))
                .and(nullableEndDateEquals(endDate))
                .and(academicSchedule.targetRole.eq(targetRole));
        if (excludedId != null) {
            predicates.and(academicSchedule.id.ne(excludedId));
        }

        return jpaQueryFactory
                .selectOne()
                .from(academicSchedule)
                .where(predicates)
                .fetchFirst() != null;
    }

    private BooleanExpression nullableTextEquals(String content) {
        return content == null ? academicSchedule.content.isNull() : academicSchedule.content.eq(content);
    }

    private BooleanExpression nullableEndDateEquals(LocalDate endDate) {
        return endDate == null ? academicSchedule.endDate.isNull() : academicSchedule.endDate.eq(endDate);
    }
}
