package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselorAvailability;
import jakarta.persistence.LockModeType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounselorAvailabilityRepository extends JpaRepository<CounselorAvailability, Long> {

    @EntityGraph(attributePaths = {"professor", "professor.user"})
    List<CounselorAvailability> findAllByOrderByProfessorIdAscDayOfWeekAscStartTimeAsc();

    @EntityGraph(attributePaths = {"professor", "professor.user"})
    List<CounselorAvailability> findByProfessorIdOrderByDayOfWeekAscStartTimeAsc(Long professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT availability
            FROM CounselorAvailability availability
            JOIN FETCH availability.professor professor
            JOIN FETCH professor.user
            WHERE professor.id = :professorId
              AND availability.dayOfWeek = :dayOfWeek
              AND availability.validFrom <= :appointmentDate
              AND (availability.validTo IS NULL OR availability.validTo >= :appointmentDate)
            ORDER BY availability.startTime
            """)
    List<CounselorAvailability> findBookableSlotsForUpdate(
            @Param("professorId") Long professorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("appointmentDate") LocalDate appointmentDate
    );

    @Modifying
    @Query("DELETE FROM CounselorAvailability availability WHERE availability.professor.id = :professorId")
    void deleteByProfessorId(@Param("professorId") Long professorId);
}
