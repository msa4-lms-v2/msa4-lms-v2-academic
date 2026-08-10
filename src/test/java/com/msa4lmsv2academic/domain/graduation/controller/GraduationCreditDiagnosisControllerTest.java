package com.msa4lmsv2academic.domain.graduation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.graduation.response.CreditDiagnosisResponseDTO;
import com.msa4lmsv2academic.domain.graduation.service.GraduationCreditDiagnosisService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GraduationCreditDiagnosisControllerTest {

    @Test
    void returnsCreditDiagnosisWithGlobalResponse() {
        CurrentUser currentUser = new CurrentUser(2001L, "STUDENT");
        CreditDiagnosisResponseDTO diagnosis = new CreditDiagnosisResponseDTO(
                1001L, 54, 32, 40, 46, 86, 6, 0, 44, false
        );
        GraduationCreditDiagnosisService service = mock(GraduationCreditDiagnosisService.class);
        when(service.diagnose(1001L, currentUser)).thenReturn(diagnosis);
        GraduationCreditDiagnosisController controller = new GraduationCreditDiagnosisController(service);

        ResponseEntity<GlobalRes<CreditDiagnosisResponseDTO>> response =
                controller.diagnose(1001L, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEqualTo(diagnosis);
    }
}
