package com.msa4lmsv2academic.domain.grade.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeManagementRepository extends JpaRepository<Enrollment, Long> {

    @Query("""
            select enrollment
            from Enrollment enrollment
            join fetch enrollment.student student
            join fetch student.user
            where enrollment.lecture.id = :classId
              and enrollment.status = com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus.ACTIVE
            order by enrollment.id
            """)
    List<Enrollment> findActiveGrades(@Param("classId") Long classId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select enrollment
            from Enrollment enrollment
            join fetch enrollment.student student
            join fetch student.user
            where enrollment.lecture.id = :classId
              and enrollment.status = com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus.ACTIVE
            order by enrollment.id
            """)
    List<Enrollment> findActiveGradesForUpdate(@Param("classId") Long classId);
}
