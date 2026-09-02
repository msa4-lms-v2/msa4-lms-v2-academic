package com.msa4lmsv2academic.domain.graduation.repository;

import static com.msa4lmsv2academic.domain.graduation.entity.QGraduationRequirement.graduationRequirement;

import com.msa4lmsv2academic.domain.graduation.entity.GraduationRequirement;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GraduationRequirementQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public GraduationRequirementSearchResult search(GraduationRequirementSearchCondition condition) {
        BooleanBuilder predicates = predicates(condition);
        List<GraduationRequirement> items = jpaQueryFactory
                .selectFrom(graduationRequirement)
                .join(graduationRequirement.department).fetchJoin()
                .where(predicates)
                .orderBy(primaryOrder(condition), graduationRequirement.id.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(graduationRequirement.count())
                .from(graduationRequirement)
                .join(graduationRequirement.department)
                .where(predicates)
                .fetchOne();
        return new GraduationRequirementSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public Optional<GraduationRequirement> findByIdWithDepartment(Long requirementId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(graduationRequirement)
                .join(graduationRequirement.department).fetchJoin()
                .where(graduationRequirement.id.eq(requirementId))
                .fetchOne());
    }

    private BooleanBuilder predicates(GraduationRequirementSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.keyword() != null) {
            predicates.and(
                    graduationRequirement.department.name.containsIgnoreCase(condition.keyword())
                            .or(graduationRequirement.department.code.eq(condition.keyword()))
            );
        }
        if (condition.departmentId() != null) {
            predicates.and(graduationRequirement.department.id.eq(condition.departmentId()));
        }
        if (condition.admissionYear() != null) {
            predicates.and(graduationRequirement.admissionYear.eq(condition.admissionYear()));
        }
        return predicates;
    }

    private OrderSpecifier<?> primaryOrder(GraduationRequirementSearchCondition condition) {
        return switch (condition.sortBy()) {
            case "admissionYear" -> condition.descending()
                    ? graduationRequirement.admissionYear.desc()
                    : graduationRequirement.admissionYear.asc();
            case "createdAt" -> condition.descending()
                    ? graduationRequirement.createdAt.desc()
                    : graduationRequirement.createdAt.asc();
            default -> condition.descending()
                    ? graduationRequirement.department.name.desc()
                    : graduationRequirement.department.name.asc();
        };
    }
}
