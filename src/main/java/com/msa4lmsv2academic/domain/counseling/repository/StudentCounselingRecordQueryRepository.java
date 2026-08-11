package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.professor.entity.QProfessor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.msa4lmsv2academic.domain.counseling.entity.QCounselingRecord.counselingRecord;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

@Repository
@RequiredArgsConstructor
public class StudentCounselingRecordQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public CounselingRecordSearchResult search(StudentCounselingRecordSearchCondition condition) {
        QUser studentUser = new QUser("currentCounselingStudentUser");
        QProfessor advisor = new QProfessor("studentCounselingAdvisor");
        QUser advisorUser = new QUser("studentCounselingAdvisorUser");

        List<CounselingRecord> items = jpaQueryFactory
                .selectFrom(counselingRecord)
                .join(counselingRecord.student, student).fetchJoin()
                .join(student.user, studentUser).fetchJoin()
                .join(counselingRecord.professor, advisor).fetchJoin()
                .join(advisor.user, advisorUser).fetchJoin()
                .where(
                        studentUser.id.eq(condition.studentUserId()),
                        counselingMethodEq(condition.counselingMethod()),
                        statusEq(condition.status())
                )
                .orderBy(counselingRecord.updatedAt.desc(), counselingRecord.id.desc())
                .offset(condition.offset())
                .limit(condition.size())
                .fetch();

        Long totalCount = jpaQueryFactory
                .select(counselingRecord.count())
                .from(counselingRecord)
                .join(counselingRecord.student, student)
                .join(student.user, studentUser)
                .where(
                        studentUser.id.eq(condition.studentUserId()),
                        counselingMethodEq(condition.counselingMethod()),
                        statusEq(condition.status())
                )
                .fetchOne();

        return new CounselingRecordSearchResult(items, totalCount == null ? 0L : totalCount);
    }

    public Optional<CounselingRecord> findById(Long recordId, Long studentUserId) {
        QUser studentUser = new QUser("counselingDetailCurrentStudentUser");
        QProfessor advisor = new QProfessor("counselingDetailAdvisor");
        QUser advisorUser = new QUser("counselingDetailAdvisorUser");

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(counselingRecord)
                .join(counselingRecord.student, student).fetchJoin()
                .join(student.user, studentUser).fetchJoin()
                .join(counselingRecord.professor, advisor).fetchJoin()
                .join(advisor.user, advisorUser).fetchJoin()
                .where(
                        counselingRecord.id.eq(recordId),
                        studentUser.id.eq(studentUserId)
                )
                .fetchOne());
    }

    public Optional<Student> findStudentWithAdvisorByUserId(Long studentUserId) {
        QUser studentUser = new QUser("onlineCounselingStudentUser");
        QProfessor advisor = new QProfessor("onlineCounselingAdvisor");
        QUser advisorUser = new QUser("onlineCounselingAdvisorUser");

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(student)
                .join(student.user, studentUser).fetchJoin()
                .leftJoin(student.advisor, advisor).fetchJoin()
                .leftJoin(advisor.user, advisorUser).fetchJoin()
                .where(studentUser.id.eq(studentUserId))
                .fetchOne());
    }

    private BooleanExpression counselingMethodEq(CounselingMethod counselingMethod) {
        return counselingMethod == null ? null : counselingRecord.counselingMethod.eq(counselingMethod);
    }

    private BooleanExpression statusEq(CounselingStatus status) {
        return status == null ? null : counselingRecord.status.eq(status);
    }
}
