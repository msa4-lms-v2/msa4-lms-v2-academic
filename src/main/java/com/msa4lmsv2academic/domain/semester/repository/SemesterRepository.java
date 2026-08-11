package com.msa4lmsv2academic.domain.semester.repository;

import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    boolean existsByAcademicYearAndTerm(short academicYear, SemesterTerm term);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select semester from Semester semester where semester.current = true")
    List<Semester> findCurrentSemestersForUpdate();
}
