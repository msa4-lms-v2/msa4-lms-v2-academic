package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.request.EnrollmentCreditLimitRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.EnrollmentCreditLimitRuleResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.EnrollmentCreditLimitRuleService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class EnrollmentCreditLimitRuleControllerTest {

    @Test
    void returnsPagedRulesWithGlobalResponse() {
        EnrollmentCreditLimitRuleSearchRequestDTO request =
                new EnrollmentCreditLimitRuleSearchRequestDTO(
                        1, 20, null, null, null, null, null
                );
        CurrentUser currentUser = new CurrentUser(3L, "ADMIN");
        PageResponseDTO<EnrollmentCreditLimitRuleResponseDTO> data =
                new PageResponseDTO<>(List.of(), 0, 1, 20, false);
        EnrollmentCreditLimitRuleService service = mock(EnrollmentCreditLimitRuleService.class);
        when(service.search(request, currentUser)).thenReturn(data);
        EnrollmentCreditLimitRuleController controller = new EnrollmentCreditLimitRuleController(service);

        ResponseEntity<? extends GlobalResponseDTO<?>> response = controller.search(request, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEqualTo(data);
    }
}
