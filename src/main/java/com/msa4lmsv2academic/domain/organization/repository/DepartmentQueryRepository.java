package com.msa4lmsv2academic.domain.organization.repository;

import com.msa4lmsv2academic.domain.organization.entity.Department;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.msa4lmsv2academic.domain.organization.entity.QDepartment.department;

@Repository
@RequiredArgsConstructor
public class DepartmentQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public DepartmentSearchResult search(DepartmentSearchCondition condition) {
        BooleanBuilder predicates = searchPredicates(condition);

        List<Department> items = jpaQueryFactory
                .selectFrom(department)
                .leftJoin(department.college).fetchJoin()
                .where(predicates)
                .orderBy(department.code.asc(), department.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(department.count())
                .from(department)
                .leftJoin(department.college)
                .where(predicates)
                .fetchOne();

        return new DepartmentSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public Optional<Department> findByIdWithCollege(Long departmentId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(department)
                .leftJoin(department.college).fetchJoin()
                .where(department.id.eq(departmentId))
                .fetchOne());
    }

    private BooleanBuilder searchPredicates(DepartmentSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.collegeId() != null) {
            predicates.and(department.college.id.eq(condition.collegeId()));
        }

        if (condition.admin()) {
            if (condition.active() != null) {
                predicates.and(department.active.eq(condition.active()));
            }
        } else {
            predicates.and(department.active.isTrue());
            predicates.and(department.college.isNull().or(department.college.active.isTrue()));
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
                department.code.eq(trimmedKeyword)
                        .or(department.name.containsIgnoreCase(trimmedKeyword))
        );
    }
}
