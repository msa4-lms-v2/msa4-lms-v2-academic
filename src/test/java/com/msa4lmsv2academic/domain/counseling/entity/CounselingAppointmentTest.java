package com.msa4lmsv2academic.domain.counseling.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CounselingAppointmentTest {

    @Test
    void confirmedAppointmentCanBeCompletedWithProfessorNote() {
        CounselingAppointment appointment = CounselingAppointment.create(
                mock(Student.class),
                mock(Professor.class),
                LocalDateTime.of(2026, 9, 7, 9, 30),
                "진로 상담"
        );

        appointment.changeStatus(CounselingAppointmentStatus.CONFIRMED, null);
        appointment.changeStatus(CounselingAppointmentStatus.COMPLETED, "상담 완료");

        assertThat(appointment.getStatus()).isEqualTo(CounselingAppointmentStatus.COMPLETED);
        assertThat(appointment.getProfessorNote()).isEqualTo("상담 완료");
    }

    @Test
    void rejectedAppointmentCannotTransitionAgain() {
        CounselingAppointment appointment = CounselingAppointment.create(
                mock(Student.class),
                mock(Professor.class),
                LocalDateTime.of(2026, 9, 7, 9, 30),
                null
        );
        appointment.changeStatus(CounselingAppointmentStatus.REJECTED, "일정 조정 필요");

        assertThatThrownBy(() -> appointment.changeStatus(CounselingAppointmentStatus.CONFIRMED, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
