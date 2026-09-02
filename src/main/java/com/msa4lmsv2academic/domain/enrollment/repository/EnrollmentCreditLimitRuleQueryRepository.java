package com.msa4lmsv2academic.domain.enrollment.repository;

import static com.msa4lmsv2academic.domain.enrollment.entity.QEnrollmentCreditLimitRule.enrollmentCreditLimitRule;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentCreditLimitRule;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentCreditLimitRuleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public EnrollmentCreditLimitRuleSearchResult search(
            EnrollmentCreditLimitRuleSearchCondition condition
    ) {
        BooleanBuilder predicates = predicates(condition);
        List<EnrollmentCreditLimitRule> items = jpaQueryFactory
                .selectFrom(enrollmentCreditLimitRule)
                .join(enrollmentCreditLimitRule.semester).fetchJoin()
                .where(predicates)
                .orderBy(
                        primaryOrder(condition),
                        enrollmentCreditLimitRule.semester.academicYear.desc(),
                        enrollmentCreditLimitRule.semester.term.desc(),
                        enrollmentCreditLimitRule.id.asc()
                )
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(enrollmentCreditLimitRule.count())
                .from(enrollmentCreditLimitRule)
                .join(enrollmentCreditLimitRule.semester)
                .where(predicates)
                .fetchOne();
        return new EnrollmentCreditLimitRuleSearchResult(
                items,
                totalCount == null ? 0 : totalCount
        );
    }

    public Optional<EnrollmentCreditLimitRule> findByIdWithSemester(Long ruleId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(enrollmentCreditLimitRule)
                .join(enrollmentCreditLimitRule.semester).fetchJoin()
                .where(enrollmentCreditLimitRule.id.eq(ruleId))
                .fetchOne());
    }

    private BooleanBuilder predicates(EnrollmentCreditLimitRuleSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.academicYear() != null) {
            predicates.and(enrollmentCreditLimitRule.semester.academicYear.eq(condition.academicYear()));
        }
        if (condition.term() != null) {
            predicates.and(enrollmentCreditLimitRule.semester.term.eq(condition.term()));
        }
        if (condition.active() != null) {
            predicates.and(enrollmentCreditLimitRule.active.eq(condition.active()));
        }
        return predicates;
    }

    private OrderSpecifier<?> primaryOrder(EnrollmentCreditLimitRuleSearchCondition condition) {
        return switch (condition.sortBy()) {
            case "maxCredits" -> condition.descending()
                    ? enrollmentCreditLimitRule.maxCredits.desc()
                    : enrollmentCreditLimitRule.maxCredits.asc();
            case "createdAt" -> condition.descending()
                    ? enrollmentCreditLimitRule.createdAt.desc()
                    : enrollmentCreditLimitRule.createdAt.asc();
            case "updatedAt" -> condition.descending()
                    ? enrollmentCreditLimitRule.updatedAt.desc()
                    : enrollmentCreditLimitRule.updatedAt.asc();
            default -> condition.descending()
                    ? enrollmentCreditLimitRule.semester.academicYear.desc()
                    : enrollmentCreditLimitRule.semester.academicYear.asc();
        };
    }
}
