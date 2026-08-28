package com.msa4lmsv2academic.domain.grade.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.grade.request.RetakeGradeReflectionRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.RetakeGradeReflectionResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.RetakeGradeReflectionService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RetakeGradeReflectionControllerTest {

    @Test
    void returnsReflectionResultWithGlobalResponse() {
        RetakeGradeReflectionService service = mock(RetakeGradeReflectionService.class);
        RetakeGradeReflectionController controller = new RetakeGradeReflectionController(service);
        RetakeGradeReflectionRequestDTO request = new RetakeGradeReflectionRequestDTO("재수강 반영");
        CurrentUser administrator = new CurrentUser(3L, "ADMIN");
        when(service.reflect(302L, request, administrator)).thenReturn(new RetakeGradeReflectionResponseDTO(
                302L, 41L, 17L, 145L, "C+", "A", 3L,
                LocalDateTime.of(2026, 8, 28, 14, 30), List.of()
        ));

        var response = controller.reflect(302L, request, administrator);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data().previousGrade()).isEqualTo("C+");
        assertThat(response.getBody().data().reflectedGrade()).isEqualTo("A");
    }
}
