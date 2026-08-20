package com.msa4lmsv2academic.domain.infochange.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProfileInfoChangeRequestTest {

    @Test
    void studentCanCancelOnlyRequestedChange() {
        StudentInfoChangeRequest request = StudentInfoChangeRequest.create(
                mock(Student.class), "변경 이름", null, null, null, null, "이름 정정"
        );
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 20, 15, 0);

        request.cancel(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(InfoChangeRequestStatus.CANCELLED);
        assertThat(request.getCancelledAt()).isEqualTo(cancelledAt);
        assertThatThrownBy(() -> request.cancel(cancelledAt.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void professorCanCancelOnlyRequestedChange() {
        ProfessorInfoChangeRequest request = ProfessorInfoChangeRequest.create(
                mock(Professor.class), null, "010-1234-5678", null, null, null, "연락처 변경"
        );
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 20, 15, 0);

        request.cancel(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(InfoChangeRequestStatus.CANCELLED);
        assertThat(request.getCancelledAt()).isEqualTo(cancelledAt);
        assertThatThrownBy(() -> request.cancel(cancelledAt.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
