package com.msa4lmsv2academic.domain.semester.repository;

import static com.msa4lmsv2academic.domain.semester.entity.QSemester.semester;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SemesterQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public SemesterSearchResult search(SemesterSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();

        if (condition.academicYear() != null) {
            predicates.and(semester.academicYear.eq(condition.academicYear()));
        }
        if (condition.term() != null) {
            predicates.and(semester.term.eq(condition.term()));
        }
        if (condition.current() != null) {
            predicates.and(semester.current.eq(condition.current()));
        }

        List<Semester> items = jpaQueryFactory
                .selectFrom(semester)
                .where(predicates)
                .orderBy(semester.academicYear.desc(), semester.term.desc(), semester.id.desc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(semester.count())
                .from(semester)
                .where(predicates)
                .fetchOne();

        return new SemesterSearchResult(items, totalCount == null ? 0 : totalCount);
    }
}
