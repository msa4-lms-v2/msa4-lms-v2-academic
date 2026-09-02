package com.msa4lmsv2academic.domain.enrollment.repository;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // 수강생 조회 메서드
    Optional<Enrollment> findByStudent_User_IdAndLecture_IdAndStatus(
            Long userId,
            Long lectureId,
            EnrollmentStatus status
    );
}
