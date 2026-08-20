package com.msa4lmsv2academic.domain.infochange.repository;

import static com.msa4lmsv2academic.domain.infochange.entity.QStudentInfoChangeRequest.studentInfoChangeRequest;

import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequest;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudentInfoChangeRequestQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public StudentInfoChangeRequestSearchResult search(InfoChangeRequestSearchCondition condition) {
        BooleanBuilder predicates = predicates(condition);
        OrderSpecifier<?> createdOrder = condition.sortDirection() == Sort.Direction.ASC
                ? studentInfoChangeRequest.createdAt.asc()
                : studentInfoChangeRequest.createdAt.desc();
        OrderSpecifier<?> idOrder = condition.sortDirection() == Sort.Direction.ASC
                ? studentInfoChangeRequest.id.asc()
                : studentInfoChangeRequest.id.desc();

        List<StudentInfoChangeRequest> items = jpaQueryFactory
                .selectFrom(studentInfoChangeRequest)
                .join(studentInfoChangeRequest.student).fetchJoin()
                .join(studentInfoChangeRequest.student.user).fetchJoin()
                .join(studentInfoChangeRequest.student.department).fetchJoin()
                .leftJoin(studentInfoChangeRequest.reviewedBy).fetchJoin()
                .where(predicates)
                .orderBy(createdOrder, idOrder)
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(studentInfoChangeRequest.count())
                .from(studentInfoChangeRequest)
                .join(studentInfoChangeRequest.student)
                .join(studentInfoChangeRequest.student.user)
                .where(predicates)
                .fetchOne();

        return new StudentInfoChangeRequestSearchResult(items, totalCount == null ? 0 : totalCount);
    }

    private BooleanBuilder predicates(InfoChangeRequestSearchCondition condition) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.requesterUserId() != null) {
            predicates.and(studentInfoChangeRequest.student.user.id.eq(condition.requesterUserId()));
        }
        if (condition.status() != null) {
            predicates.and(studentInfoChangeRequest.status.eq(condition.status()));
        }
        if (condition.departmentId() != null) {
            predicates.and(studentInfoChangeRequest.student.department.id.eq(condition.departmentId()));
        }
        if (condition.keyword() != null) {
            predicates.and(studentInfoChangeRequest.student.user.name.containsIgnoreCase(condition.keyword()));
        }
        return predicates;
    }
}
