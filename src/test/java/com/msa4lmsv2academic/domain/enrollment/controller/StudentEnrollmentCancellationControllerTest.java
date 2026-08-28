package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.entity.EnrollmentStatus;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentCancellationResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentCancellationService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StudentEnrollmentCancellationControllerTest {

    @Test
    void returnsCancelledStatusWithGlobalResponse() {
        StudentEnrollmentCancellationService service = mock(StudentEnrollmentCancellationService.class);
        StudentEnrollmentCancellationController controller =
                new StudentEnrollmentCancellationController(service);
        CurrentUser currentUser = new CurrentUser(10L, "STUDENT");
        LocalDateTime cancelledAt = LocalDateTime.of(2026, 8, 28, 10, 30);
        when(service.cancel(40L, currentUser)).thenReturn(
                new StudentEnrollmentCancellationResponseDTO(
                        40L, 20L, 30L, EnrollmentStatus.CANCELLED, cancelledAt
                )
        );

        var response = controller.cancel(40L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data().status()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(response.getBody().data().cancelledAt()).isEqualTo(cancelledAt);
    }
}
