package com.msa4lmsv2academic.domain.evaluation.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface LectureEvaluationEnrollmentRepository extends Repository<Enrollment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select enrollment
            from Enrollment enrollment
            join fetch enrollment.student student
            join fetch student.user user
            join fetch enrollment.lecture lecture
            join fetch lecture.semester semester
            where enrollment.id = :enrollmentId
              and user.id = :studentUserId
            """)
    Optional<Enrollment> findOwnedEnrollmentForUpdate(
            @Param("enrollmentId") Long enrollmentId,
            @Param("studentUserId") Long studentUserId
    );
}
