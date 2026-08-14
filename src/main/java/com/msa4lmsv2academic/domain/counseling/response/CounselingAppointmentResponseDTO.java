package com.msa4lmsv2academic.domain.counseling.response;

import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointment;
import com.msa4lmsv2academic.domain.counseling.entity.CounselingAppointmentStatus;
import java.time.LocalDateTime;

public record CounselingAppointmentResponseDTO(
        Long id,
        Long studentId,
        String studentName,
        Long professorId,
        String professorName,
        LocalDateTime appointmentAt,
        String topic,
        CounselingAppointmentStatus status,
        String professorNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CounselingAppointmentResponseDTO from(CounselingAppointment appointment) {
        return new CounselingAppointmentResponseDTO(
                appointment.getId(),
                appointment.getStudent().getId(),
                appointment.getStudent().getUser().getName(),
                appointment.getProfessor().getId(),
                appointment.getProfessor().getUser().getName(),
                appointment.getAppointmentAt(),
                appointment.getTopic(),
                appointment.getStatus(),
                appointment.getProfessorNote(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
