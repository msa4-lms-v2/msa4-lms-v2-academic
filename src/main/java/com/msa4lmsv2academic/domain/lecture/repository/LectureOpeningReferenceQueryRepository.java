package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.course.entity.Course;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LectureOpeningReferenceQueryRepository {

    private final EntityManager entityManager;

    public Optional<Course> findCourseById(Long courseId) {
        return Optional.ofNullable(entityManager.find(Course.class, courseId));
    }

    public Optional<Semester> findSemesterById(Long semesterId) {
        return Optional.ofNullable(entityManager.find(Semester.class, semesterId));
    }

    public Optional<Professor> findProfessorByUserId(Long userId) {
        return entityManager.createQuery(
                        "select professor from Professor professor "
                                + "join fetch professor.user user "
                                + "where user.id = :userId",
                        Professor.class
                )
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    public Optional<User> findUserById(Long userId) {
        return Optional.ofNullable(entityManager.find(User.class, userId));
    }

    public Optional<Professor> lockProfessor(Long professorId) {
        return entityManager.createQuery(
                        "select professor from Professor professor where professor.id = :professorId",
                        Professor.class
                )
                .setParameter("professorId", professorId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}
