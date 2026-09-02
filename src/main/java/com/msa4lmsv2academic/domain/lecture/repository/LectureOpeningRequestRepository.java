package com.msa4lmsv2academic.domain.lecture.repository;

import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequest;
import com.msa4lmsv2academic.domain.lecture.entity.LectureOpeningRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureOpeningRequestRepository extends JpaRepository<LectureOpeningRequest, Long> {

    boolean existsByCourseIdAndProfessorIdAndSemesterIdAndSectionNoAndStatus(
            Long courseId,
            Long professorId,
            Long semesterId,
            String sectionNo,
            LectureOpeningRequestStatus status
    );

    boolean existsByCourseIdAndProfessorIdAndSemesterIdAndSectionNoAndStatusAndIdNot(
            Long courseId,
            Long professorId,
            Long semesterId,
            String sectionNo,
            LectureOpeningRequestStatus status,
            Long id
    );

    @EntityGraph(attributePaths = {"course", "professor", "professor.user", "semester", "reviewedBy", "approvedLecture"})
    Page<LectureOpeningRequest> findByProfessorUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "professor", "professor.user", "semester", "reviewedBy", "approvedLecture"})
    Page<LectureOpeningRequest> findByProfessorUserIdAndStatus(
            Long userId,
            LectureOpeningRequestStatus status,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"course", "professor", "professor.user", "semester", "reviewedBy", "approvedLecture"})
    Page<LectureOpeningRequest> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"course", "professor", "professor.user", "semester", "reviewedBy", "approvedLecture"})
    Page<LectureOpeningRequest> findByStatus(LectureOpeningRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "course", "professor", "professor.user", "semester", "reviewedBy", "approvedLecture", "schedules"
    })
    Optional<LectureOpeningRequest> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"course", "professor", "professor.user", "semester", "schedules"})
    @Query("select request from LectureOpeningRequest request where request.id = :requestId")
    Optional<LectureOpeningRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}
