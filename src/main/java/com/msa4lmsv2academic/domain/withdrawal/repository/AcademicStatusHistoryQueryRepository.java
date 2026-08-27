package com.msa4lmsv2academic.domain.withdrawal.repository;

import com.msa4lmsv2academic.domain.organization.entity.QDepartment;
import com.msa4lmsv2academic.domain.student.entity.QStudent;
import com.msa4lmsv2academic.domain.student.repository.StudentQueryRepository;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.msa4lmsv2academic.domain.withdrawal.entity.AcademicStatusHistory;
import com.msa4lmsv2academic.domain.withdrawal.entity.QAcademicStatusHistory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AcademicStatusHistoryQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final StudentQueryRepository studentQueryRepository;

    public Page<AcademicStatusHistory> search(AcademicStatusHistorySearchCondition condition, Pageable pageable) {
        QAcademicStatusHistory history = QAcademicStatusHistory.academicStatusHistory;
        QStudent historyStudent = new QStudent("historyStudent");
        QUser studentUser = new QUser("historyStudentUser");
        QDepartment department = new QDepartment("historyDepartment");
        BooleanBuilder predicates = predicates(condition, history, historyStudent, studentUser);
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        boolean isAscending = order != null && order.isAscending();

        List<AcademicStatusHistory> items = jpaQueryFactory.selectFrom(history)
                .join(history.student, historyStudent).fetchJoin()
                .join(historyStudent.user, studentUser).fetchJoin()
                .join(historyStudent.department, department).fetchJoin()
                .where(predicates)
                .orderBy(isAscending ? history.createdAt.asc() : history.createdAt.desc(),
                        isAscending ? history.id.asc() : history.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long totalCount = jpaQueryFactory.select(history.count())
                .from(history)
                .join(history.student, historyStudent)
                .join(historyStudent.user, studentUser)
                .join(historyStudent.department, department)
                .where(predicates)
                .fetchOne();
        return new PageImpl<>(items, pageable, totalCount == null ? 0 : totalCount);
    }

    private BooleanBuilder predicates(AcademicStatusHistorySearchCondition condition,
                                      QAcademicStatusHistory history, QStudent historyStudent, QUser studentUser) {
        BooleanBuilder predicates = new BooleanBuilder();
        if (condition.ownerUserId() != null) {
            predicates.and(studentUser.id.eq(condition.ownerUserId()));
        }
        if (condition.professorScope() != null) {
            predicates.and(historyStudent.id.in(
                    studentQueryRepository.studentIdsInProfessorScope(condition.professorScope())
            ));
        }
        if (condition.keyword() != null) {
            predicates.and(studentUser.name.containsIgnoreCase(condition.keyword()));
        }
        if (condition.studentId() != null) {
            predicates.and(historyStudent.id.eq(condition.studentId()));
        }
        if (condition.departmentId() != null) {
            predicates.and(historyStudent.department.id.eq(condition.departmentId()));
        }
        if (condition.previousStatus() != null) {
            predicates.and(history.previousStatus.eq(condition.previousStatus()));
        }
        if (condition.newStatus() != null) {
            predicates.and(history.newStatus.eq(condition.newStatus()));
        }
        if (condition.sourceType() != null) {
            predicates.and(history.sourceType.eq(condition.sourceType().name()));
        }
        if (condition.fromDate() != null) {
            predicates.and(history.createdAt.goe(condition.fromDate().atStartOfDay()));
        }
        // MySQL DATETIME 최댓날에는 상한이 필요 없다. 나노초 최댓값도 JDBC 반올림으로 넘칠 수 있다.
        if (condition.toDate() != null && !condition.toDate().equals(LocalDate.of(9999, 12, 31))) {
            predicates.and(history.createdAt.lt(condition.toDate().plusDays(1).atStartOfDay()));
        }
        return predicates;
    }
}
