package com.msa4lmsv2academic.domain.withdrawal.repository;

import static com.msa4lmsv2academic.domain.student.entity.QStudent.student;
import static com.msa4lmsv2academic.domain.user.entity.QUser.user;

import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WithdrawalQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Optional<Student> findStudentByUserIdForUpdate(Long userId) {
        return Optional.ofNullable(queryFactory.selectFrom(student).where(student.user.id.eq(userId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).fetchOne());
    }

    public Optional<Student> findStudentByIdForUpdate(Long studentId) {
        return Optional.ofNullable(queryFactory.selectFrom(student).where(student.id.eq(studentId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).fetchOne());
    }

    public Optional<User> findUserById(Long userId) {
        return Optional.ofNullable(queryFactory.selectFrom(user).where(user.id.eq(userId)).fetchOne());
    }
}

