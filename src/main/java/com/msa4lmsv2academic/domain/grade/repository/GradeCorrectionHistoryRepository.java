package com.msa4lmsv2academic.domain.grade.repository;

import com.msa4lmsv2academic.domain.grade.entity.GradeCorrectionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeCorrectionHistoryRepository extends JpaRepository<GradeCorrectionHistory, Long> {

    boolean existsByEnrollmentIdAndFieldChangedAndNewValue(
            Long enrollmentId,
            String fieldChanged,
            String newValue
    );

    @Query(
            value = """
                    select history
                    from GradeCorrectionHistory history
                    join fetch history.enrollment enrollment
                    join fetch enrollment.student student
                    join fetch student.user
                    join fetch history.changedBy
                    where enrollment.lecture.id = :classId
                    """,
            countQuery = """
                    select count(history)
                    from GradeCorrectionHistory history
                    where history.enrollment.lecture.id = :classId
                    """
    )
    Page<GradeCorrectionHistory> findByClassId(
            @Param("classId") Long classId,
            Pageable pageable
    );
}
