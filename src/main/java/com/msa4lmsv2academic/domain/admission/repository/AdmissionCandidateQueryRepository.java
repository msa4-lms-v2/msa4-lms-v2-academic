package com.msa4lmsv2academic.domain.admission.repository;

import static com.msa4lmsv2academic.domain.admission.entity.QAdmissionCandidate.admissionCandidate;

import com.msa4lmsv2academic.domain.admission.entity.AdmissionCandidate;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdmissionCandidateQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public AdmissionCandidateSearchResult search(AdmissionCandidateSearchCondition condition) {
        BooleanBuilder predicates = predicates(condition);

        List<AdmissionCandidate> items = jpaQueryFactory
                .selectFrom(admissionCandidate)
                .join(admissionCandidate.department).fetchJoin()
                .where(predicates)
                .orderBy(primaryOrder(condition), idOrder(condition))
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(admissionCandidate.count())
                .from(admissionCandidate)
                .where(predicates)
                .fetchOne();

        return new AdmissionCandidateSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    public Optional<AdmissionCandidate> findByIdWithDetails(Long candidateId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(admissionCandidate)
                .join(admissionCandidate.department).fetchJoin()
                .join(admissionCandidate.createdBy).fetchJoin()
                .leftJoin(admissionCandidate.statusChangedBy).fetchJoin()
                .leftJoin(admissionCandidate.student).fetchJoin()
                .where(admissionCandidate.id.eq(candidateId))
                .fetchOne());
    }

    private BooleanBuilder predicates(AdmissionCandidateSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.keyword() != null) {
            predicates.and(
                    admissionCandidate.name.containsIgnoreCase(condition.keyword())
                            .or(admissionCandidate.applicationNumber.containsIgnoreCase(condition.keyword()))
            );
        }
        if (condition.departmentId() != null) {
            predicates.and(admissionCandidate.department.id.eq(condition.departmentId()));
        }
        if (condition.admissionYear() != null) {
            predicates.and(admissionCandidate.admissionYear.eq(condition.admissionYear()));
        }
        if (condition.status() != null) {
            predicates.and(admissionCandidate.status.eq(condition.status()));
        }
        return predicates;
    }

    private OrderSpecifier<?> primaryOrder(AdmissionCandidateSearchCondition condition) {
        return switch (condition.sortBy()) {
            case "name" -> condition.descending()
                    ? admissionCandidate.name.desc()
                    : admissionCandidate.name.asc();
            case "applicationNumber" -> condition.descending()
                    ? admissionCandidate.applicationNumber.desc()
                    : admissionCandidate.applicationNumber.asc();
            case "admissionYear" -> condition.descending()
                    ? admissionCandidate.admissionYear.desc()
                    : admissionCandidate.admissionYear.asc();
            default -> condition.descending()
                    ? admissionCandidate.createdAt.desc()
                    : admissionCandidate.createdAt.asc();
        };
    }

    private OrderSpecifier<Long> idOrder(AdmissionCandidateSearchCondition condition) {
        return condition.descending() ? admissionCandidate.id.desc() : admissionCandidate.id.asc();
    }
}
