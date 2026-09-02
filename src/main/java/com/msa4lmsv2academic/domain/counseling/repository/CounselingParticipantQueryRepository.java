package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CounselingParticipantQueryRepository {

    private final EntityManager entityManager;

    public Optional<Student> findStudentByUserIdForUpdate(Long userId) {
        return entityManager.createQuery("""
                        SELECT student
                        FROM Student student
                        JOIN FETCH student.user
                        WHERE student.user.id = :userId
                        """, Student.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    public Optional<Professor> findProfessorByUserIdForUpdate(Long userId) {
        return entityManager.createQuery("""
                        SELECT professor
                        FROM Professor professor
                        JOIN FETCH professor.user
                        WHERE professor.user.id = :userId
                        """, Professor.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    public Optional<Professor> findProfessorByUserId(Long userId) {
        return entityManager.createQuery("""
                        SELECT professor
                        FROM Professor professor
                        JOIN FETCH professor.user
                        WHERE professor.user.id = :userId
                        """, Professor.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    public Optional<Professor> findProfessorById(Long professorId) {
        return entityManager.createQuery("""
                        SELECT professor
                        FROM Professor professor
                        JOIN FETCH professor.user
                        WHERE professor.id = :professorId
                        """, Professor.class)
                .setParameter("professorId", professorId)
                .getResultStream()
                .findFirst();
    }
}
