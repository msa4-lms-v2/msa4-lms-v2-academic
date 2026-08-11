package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingMethod;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingRecord;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.msa4lmsv2academic.domain.counseling.entity.QCounselingRecord.counselingRecord;
import static com.msa4lmsv2academic.domain.professor.entity.QProfessor.professor;
import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;

@Repository
@RequiredArgsConstructor
public class ProfessorCounselingRecordQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public CounselingRecordSearchResult search(CounselingRecordSearchCondition condition) {
        QUser studentUser = new QUser("counselingStudentUser");
        QUser professorUser = new QUser("counselingProfessorUser");

        List<CounselingRecord> items = jpaQueryFactory
                .selectFrom(counselingRecord)
                .join(counselingRecord.student, student).fetchJoin()
                .join(student.user, studentUser).fetchJoin()
                .join(counselingRecord.professor, professor).fetchJoin()
                .join(professor.user, professorUser).fetchJoin()
                .where(
                        professorUser.id.eq(condition.professorUserId()),
                        studentIdEq(condition.studentId()),
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
                .join(counselingRecord.professor, professor)
                .join(professor.user, professorUser)
                .where(
                        professorUser.id.eq(condition.professorUserId()),
                        studentIdEq(condition.studentId()),
                        counselingMethodEq(condition.counselingMethod()),
                        statusEq(condition.status())
                )
                .fetchOne();

        return new CounselingRecordSearchResult(items, totalCount == null ? 0L : totalCount);
    }

    public Optional<CounselingRecord> findByIdForProfessor(Long recordId, Long professorUserId) {
        QUser studentUser = new QUser("counselingDetailStudentUser");
        QUser professorUser = new QUser("counselingDetailProfessorUser");

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(counselingRecord)
                .join(counselingRecord.student, student).fetchJoin()
                .join(student.user, studentUser).fetchJoin()
                .join(counselingRecord.professor, professor).fetchJoin()
                .join(professor.user, professorUser).fetchJoin()
                .where(
                        counselingRecord.id.eq(recordId),
                        professorUser.id.eq(professorUserId)
                )
                .fetchOne());
    }

    public Optional<Professor> findProfessorByUserId(Long professorUserId) {
        QUser professorUser = new QUser("currentProfessorUser");

        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(professor)
                .join(professor.user, professorUser).fetchJoin()
                .where(professorUser.id.eq(professorUserId))
                .fetchOne());
    }

    public Optional<Student> findAdvisedStudent(Long studentId, Long professorId) {
        return Optional.ofNullable(jpaQueryFactory
                .selectFrom(student)
                .join(student.user).fetchJoin()
                .where(
                        student.id.eq(studentId),
                        student.advisor.id.eq(professorId)
                )
                .fetchOne());
    }

    private BooleanExpression studentIdEq(Long studentId) {
        return studentId == null ? null : counselingRecord.student.id.eq(studentId);
    }

    private BooleanExpression counselingMethodEq(CounselingMethod counselingMethod) {
        return counselingMethod == null ? null : counselingRecord.counselingMethod.eq(counselingMethod);
    }

    private BooleanExpression statusEq(CounselingStatus status) {
        return status == null ? null : counselingRecord.status.eq(status);
    }
}
