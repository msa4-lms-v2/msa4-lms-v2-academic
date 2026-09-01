package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.Lecture;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    boolean existsBySemesterIdAndCourseIdAndSectionNo(Long semesterId, Long courseId, String sectionNo);

    @Query("""
            select lecture
            from Lecture lecture
            join fetch lecture.professor professor
            join fetch professor.user
            where lecture.id = :classId
            """)
    Optional<Lecture> findSyllabusById(@Param("classId") Long classId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select lecture
            from Lecture lecture
            join fetch lecture.professor professor
            join fetch professor.user
            where lecture.id = :classId
            """)
    Optional<Lecture> findSyllabusByIdForUpdate(@Param("classId") Long classId);

    Optional<Lecture> findByIdAndProfessor_User_IdAndSemester_CurrentTrue(
            Long classId,
            Long userId
    );
}
