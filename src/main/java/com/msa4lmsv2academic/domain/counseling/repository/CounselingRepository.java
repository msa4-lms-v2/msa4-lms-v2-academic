package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.Counseling;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounselingRepository extends JpaRepository<Counseling, Long> {

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "professor", "professor.user"})
    Page<Counseling> findByStudentUserIdAndStatusIn(Long userId, Collection<CounselingStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "professor", "professor.user"})
    Page<Counseling> findByProfessorUserIdAndStatusIn(Long userId, Collection<CounselingStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "professor", "professor.user"})
    Page<Counseling> findByStatusIn(Collection<CounselingStatus> statuses, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "professor", "professor.user"})
    Optional<Counseling> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "student.user", "student.department", "professor", "professor.user"})
    @Query("select counseling from Counseling counseling where counseling.id = :counselingId")
    Optional<Counseling> findByIdForUpdate(@Param("counselingId") Long counselingId);
}
