package com.msa4lmsv2academic.domain.grade.repository;

import com.msa4lmsv2academic.domain.grade.entity.StudentGradeSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGradeSummaryRepository extends JpaRepository<StudentGradeSummary, Long> {

    List<StudentGradeSummary> findAllByStudentId(Long studentId);
}
