package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartCreateRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCartSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartCreateResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCartSummaryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentCartService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class EnrollmentCartControllerTest {

    @Test
    void returnsCartSummaryWithGlobalResponse() {
        EnrollmentCartService service = mock(EnrollmentCartService.class);
        EnrollmentCartController controller = new EnrollmentCartController(service);
        EnrollmentCartSearchRequestDTO request = new EnrollmentCartSearchRequestDTO((short) 2026, null);
        CurrentUser currentUser = new CurrentUser(10L, "STUDENT");
        when(service.getMyCart(request, currentUser))
                .thenReturn(new EnrollmentCartSummaryResponseDTO(0, List.of()));

        var response = controller.getMyCart(request, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data().items()).isEmpty();
    }

    @Test
    void returnsCreatedWhenCartItemIsAdded() {
        EnrollmentCartService service = mock(EnrollmentCartService.class);
        EnrollmentCartController controller = new EnrollmentCartController(service);
        EnrollmentCartCreateRequestDTO request = new EnrollmentCartCreateRequestDTO(30L);
        CurrentUser currentUser = new CurrentUser(10L, "STUDENT");
        when(service.add(request, currentUser)).thenReturn(
                new EnrollmentCartCreateResponseDTO(40L, 20L, 30L, LocalDateTime.of(2026, 8, 28, 9, 0))
        );

        var response = controller.add(request, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().cartItemId()).isEqualTo(40L);
    }

    @Test
    void removesOwnedCartItem() {
        EnrollmentCartService service = mock(EnrollmentCartService.class);
        EnrollmentCartController controller = new EnrollmentCartController(service);
        CurrentUser currentUser = new CurrentUser(10L, "STUDENT");

        var response = controller.remove(40L, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isNull();
        verify(service).remove(40L, currentUser);
    }
}
