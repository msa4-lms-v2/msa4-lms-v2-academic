package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SyllabusFileReferenceQueryRepository {

    private final EntityManager entityManager;

    public Optional<Lecture> findLecture(Long classId) {
        return entityManager.createQuery("""
                        select lecture
                        from Lecture lecture
                        join fetch lecture.professor professor
                        join fetch professor.user
                        where lecture.id = :classId
                        """, Lecture.class)
                .setParameter("classId", classId)
                .getResultStream()
                .findFirst();
    }

    public Optional<Lecture> findLectureForUpdate(Long classId) {
        return Optional.ofNullable(entityManager.find(Lecture.class, classId, LockModeType.PESSIMISTIC_WRITE));
    }

    public Optional<User> findUser(Long userId) {
        return Optional.ofNullable(entityManager.find(User.class, userId));
    }
}
