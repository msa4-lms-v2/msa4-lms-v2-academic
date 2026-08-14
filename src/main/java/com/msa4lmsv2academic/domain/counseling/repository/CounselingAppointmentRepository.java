package com.msa4lmsv2academic.domain.counseling.repository;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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

    boolean existsByProfessorIdAndAppointmentAt(Long professorId, LocalDateTime appointmentAt);

    boolean existsByStudentIdAndAppointmentAt(Long studentId, LocalDateTime appointmentAt);
}
