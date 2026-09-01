package com.msa4lmsv2academic.domain.attendance.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.msa4lmsv2academic.domain.enrollment.entity.Enrollment;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExcuseRequestTest {

    @Test
    void createsPendingRequestWithApplicationValues() {
        Enrollment enrollment = Mockito.mock(Enrollment.class);
        LocalDate lectureDate = LocalDate.of(2026, 9, 1);

        ExcuseRequest request = ExcuseRequest.create(
                enrollment,
                lectureDate,
                (byte) 2,
                "병원 진료"
        );

        assertThat(request.getEnrollment()).isSameAs(enrollment);
        assertThat(request.getLectureDate()).isEqualTo(lectureDate);
        assertThat(request.getPeriod()).isEqualTo((byte) 2);
        assertThat(request.getReason()).isEqualTo("병원 진료");
        assertThat(request.getStatus()).isEqualTo(ExcuseRequestStatus.PENDING);
        assertThat(request.getRejectReason()).isNull();
        assertThat(request.getAttachmentStoredName()).isNull();
    }
}
