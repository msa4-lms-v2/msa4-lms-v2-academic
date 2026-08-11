package com.msa4lmsv2academic.domain.counseling.entity;

import com.msa4lmsv2academic.domain.professor.entity.Professor;
import com.msa4lmsv2academic.domain.student.entity.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CounselingRecordTest {

    private final Student student = mock(Student.class);
    private final Professor professor = mock(Professor.class);

    @Test
    void onlineRequestStartsPendingAndBecomesCompletedWhenProfessorAnswers() {
        CounselingRecord record = CounselingRecord.requestOnline(
                student,
                professor,
                "졸업 요건 상담",
                "남은 필수 학점을 확인하고 싶습니다."
        );
        LocalDateTime answeredAt = LocalDateTime.of(2026, 8, 11, 10, 30);

        assertThat(record.getCounselingMethod()).isEqualTo(CounselingMethod.ONLINE);
        assertThat(record.getStatus()).isEqualTo(CounselingStatus.PENDING);
        assertThat(record.getProfessorResponse()).isNull();

        record.answer("전공필수 6학점이 더 필요합니다.", answeredAt);

        assertThat(record.getStatus()).isEqualTo(CounselingStatus.COMPLETED);
        assertThat(record.getProfessorResponse()).isEqualTo("전공필수 6학점이 더 필요합니다.");
        assertThat(record.getRespondedAt()).isEqualTo(answeredAt);
        assertThat(record.getCounseledAt()).isEqualTo(answeredAt);
    }

    @Test
    void inPersonRecordIsCompletedWithoutOnlineStudentContent() {
        LocalDateTime counseledAt = LocalDateTime.of(2026, 8, 10, 14, 0);

        CounselingRecord record = CounselingRecord.recordInPerson(
                student,
                professor,
                "복수전공 상담",
                "신청 일정과 필요 서류를 안내했습니다.",
                counseledAt
        );

        assertThat(record.getCounselingMethod()).isEqualTo(CounselingMethod.IN_PERSON);
        assertThat(record.getStatus()).isEqualTo(CounselingStatus.COMPLETED);
        assertThat(record.getStudentContent()).isNull();
        assertThat(record.getProfessorResponse()).isEqualTo("신청 일정과 필요 서류를 안내했습니다.");
        assertThat(record.getCounseledAt()).isEqualTo(counseledAt);
    }

    @Test
    void inPersonRecordCannotReceiveOnlineAnswer() {
        CounselingRecord record = CounselingRecord.recordInPerson(
                student,
                professor,
                "진로 상담",
                "진로 방향을 논의했습니다.",
                LocalDateTime.of(2026, 8, 10, 14, 0)
        );

        assertThatThrownBy(() -> record.answer("답변", LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("온라인 상담에만 답변할 수 있습니다.");
    }
}
