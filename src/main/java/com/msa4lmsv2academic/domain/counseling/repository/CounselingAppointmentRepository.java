package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounselingAppointmentRepository extends JpaRepository<CounselingAppointment, Long> {

    @EntityGraph(attributePaths = {"student", "student.user", "professor", "professor.user"})
    Page<CounselingAppointment> findByStudentUserIdAndStatusIn(
            Long studentUserId,
            Collection<CounselingAppointmentStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student", "student.user", "professor", "professor.user"})
    Page<CounselingAppointment> findByProfessorUserIdAndStatusIn(
            Long professorUserId,
            Collection<CounselingAppointmentStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student", "student.user", "professor", "professor.user"})
    Page<CounselingAppointment> findByStatusIn(
            Collection<CounselingAppointmentStatus> statuses,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "professor", "professor.user"})
    Optional<CounselingAppointment> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "student.user", "professor", "professor.user"})
    @Query("select appointment from CounselingAppointment appointment where appointment.id = :appointmentId")
    Optional<CounselingAppointment> findByIdForUpdate(@Param("appointmentId") Long appointmentId);

    boolean existsByProfessorIdAndAppointmentAt(Long professorId, LocalDateTime appointmentAt);

    boolean existsByStudentIdAndAppointmentAt(Long studentId, LocalDateTime appointmentAt);
}
