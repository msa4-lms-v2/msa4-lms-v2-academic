package com.msa4lmsv2academic.domain.attendance.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void replacesAttachmentMetadataWhilePending() {
        ExcuseRequest request = ExcuseRequest.create(
                Mockito.mock(Enrollment.class),
                LocalDate.of(2026, 9, 1),
                (byte) 2,
                "병원 진료"
        );

        request.replaceAttachment(
                "진료확인서.pdf",
                "excuse-requests/31/file.pdf",
                "application/pdf",
                2048L
        );

        assertThat(request.hasAttachment()).isTrue();
        assertThat(request.getAttachmentOriginalName()).isEqualTo("진료확인서.pdf");
        assertThat(request.getAttachmentStoredName()).isEqualTo("excuse-requests/31/file.pdf");
        assertThat(request.getAttachmentContentType()).isEqualTo("application/pdf");
        assertThat(request.getAttachmentSize()).isEqualTo(2048L);
    }

    @Test
    void approvesPendingRequestAndClearsRejectReason() {
        ExcuseRequest request = request();

        request.approve();

        assertThat(request.getStatus()).isEqualTo(ExcuseRequestStatus.APPROVED);
        assertThat(request.getRejectReason()).isNull();
    }

    @Test
    void rejectsPendingRequestWithTrimmedReason() {
        ExcuseRequest request = request();

        request.reject("  증빙 자료를 확인할 수 없습니다.  ");

        assertThat(request.getStatus()).isEqualTo(ExcuseRequestStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("증빙 자료를 확인할 수 없습니다.");
    }

    @Test
    void rejectsBlankReasonAndRepeatedReview() {
        ExcuseRequest request = request();

        assertThatThrownBy(() -> request.reject("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("반려 사유");

        request.approve();
        assertThatThrownBy(request::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 대기");
    }

    private ExcuseRequest request() {
        return ExcuseRequest.create(
                Mockito.mock(Enrollment.class),
                LocalDate.of(2026, 9, 1),
                (byte) 2,
                "병원 진료"
        );
    }
}
