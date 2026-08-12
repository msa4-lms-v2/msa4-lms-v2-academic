package com.msa4lmsv2academic.domain.professor.repository;

import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfessorQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public ProfessorSearchResult search(ProfessorSearchCondition condition) {
        BooleanBuilder predicates = searchPredicates(condition);

        List<Professor> items = jpaQueryFactory
                .selectFrom(professor)
                .join(professor.user).fetchJoin()
                .join(professor.department).fetchJoin()
                .where(predicates)
                .orderBy(professor.user.name.asc(), professor.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(professor.count())
                .from(professor)
                .join(professor.user)
                .join(professor.department)
                .where(predicates)
                .fetchOne();

        return new ProfessorSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public Optional<Professor> findByIdWithDetails(Long professorId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(professor)
                .join(professor.user).fetchJoin()
                .join(professor.department).fetchJoin()
                .where(professor.id.eq(professorId))
                .fetchOne());
    }

    private BooleanBuilder searchPredicates(ProfessorSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.departmentId() != null) {
            predicates.and(professor.department.id.eq(condition.departmentId()));
        }
        if (condition.hireYear() != null) {
            predicates.and(professor.hireYear.eq(condition.hireYear()));
        }
        if (condition.status() != null) {
            predicates.and(professor.user.status.eq(condition.status()));
        }
        keywordPredicate(condition.keyword()).ifPresent(predicates::and);
        return predicates;
    }

    private Optional<BooleanExpression> keywordPredicate(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }
        String trimmedKeyword = keyword.trim();
        return Optional.of(
                professor.user.name.containsIgnoreCase(trimmedKeyword)
                        .or(professor.user.email.containsIgnoreCase(trimmedKeyword))
        );
    }
}
