package com.msa4lmsv2academic.domain.infochange.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProfileInfoChangeRequestTest {

    private static final ProfileSnapshot PREVIOUS_PROFILE = new ProfileSnapshot(
            "기존 이름", "010-0000-0000", "before@example.com", "서울특별시", null
    );

    @Test
    void studentCanCancelOnlyRequestedChange() {
        StudentInfoChangeRequest request = StudentInfoChangeRequest.create(
                mock(Student.class), "변경 이름", null, null, null, null, PREVIOUS_PROFILE, "이름 정정"
        );
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 20, 15, 0);

        request.cancel(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(InfoChangeRequestStatus.CANCELLED);
        assertThat(request.getPreviousName()).isEqualTo("기존 이름");
        assertThat(request.getCancelledAt()).isEqualTo(cancelledAt);
        assertThatThrownBy(() -> request.cancel(cancelledAt.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void professorCanCancelOnlyRequestedChange() {
        ProfessorInfoChangeRequest request = ProfessorInfoChangeRequest.create(
                mock(Professor.class), null, "010-1234-5678", null, null, null, PREVIOUS_PROFILE, "연락처 변경"
        );
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 20, 15, 0);

        request.cancel(cancelledAt);

        assertThat(request.getStatus()).isEqualTo(InfoChangeRequestStatus.CANCELLED);
        assertThat(request.getPreviousPhoneNumber()).isEqualTo("010-0000-0000");
        assertThat(request.getCancelledAt()).isEqualTo(cancelledAt);
        assertThatThrownBy(() -> request.cancel(cancelledAt.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
