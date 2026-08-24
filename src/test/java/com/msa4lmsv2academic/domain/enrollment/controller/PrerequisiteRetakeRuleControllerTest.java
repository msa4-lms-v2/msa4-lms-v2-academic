package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.request.PrerequisiteRetakeRuleSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.PrerequisiteRetakeRuleQueryResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.PrerequisiteRetakeRuleService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PrerequisiteRetakeRuleControllerTest {

    @Test
    void returnsCriteriaAndEvaluationWithGlobalResponse() {
        PrerequisiteRetakeRuleSearchRequestDTO request = new PrerequisiteRetakeRuleSearchRequestDTO(
                1, 20, null, 20L, null, null, null, null
        );
        CurrentUser currentUser = new CurrentUser(18L, "STUDENT");
        PrerequisiteRetakeRuleQueryResponseDTO data = new PrerequisiteRetakeRuleQueryResponseDTO(
                new PageResponseDTO<>(List.of(), 0, 1, 20, false),
                null
        );
        PrerequisiteRetakeRuleService service = mock(PrerequisiteRetakeRuleService.class);
        when(service.search(request, currentUser)).thenReturn(data);
        PrerequisiteRetakeRuleController controller = new PrerequisiteRetakeRuleController(service);

        ResponseEntity<GlobalResponseDTO<PrerequisiteRetakeRuleQueryResponseDTO>> response =
                controller.search(request, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEqualTo(data);
    }
}
