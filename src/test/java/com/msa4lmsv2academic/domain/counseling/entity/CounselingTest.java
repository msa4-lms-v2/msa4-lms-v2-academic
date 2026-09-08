package com.msa4lmsv2academic.domain.counseling.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CounselingTest {
    @Test
    void startsWaitingAndBecomesAnswered() {
        Counseling counseling = Counseling.create(mock(Student.class), mock(Professor.class), "진로 상담", "질문");
        assertThat(counseling.getStatus()).isEqualTo(CounselingStatus.WAITING);
        assertThat(counseling.hasAnswer()).isFalse();
        LocalDateTime answeredAt = LocalDateTime.of(2026, 9, 8, 14, 0);
        counseling.answer("답변", answeredAt);
        assertThat(counseling.getStatus()).isEqualTo(CounselingStatus.ANSWERED);
        assertThat(counseling.getAnswer()).isEqualTo("답변");
        assertThat(counseling.getAnsweredAt()).isEqualTo(answeredAt);
    }
}
