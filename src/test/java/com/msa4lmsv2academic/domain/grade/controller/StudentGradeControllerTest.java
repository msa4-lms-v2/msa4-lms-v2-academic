package com.msa4lmsv2academic.domain.grade.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.grade.request.StudentGradeSearchRequestDTO;
import com.msa4lmsv2academic.domain.grade.response.StudentGradeResponseDTO;
import com.msa4lmsv2academic.domain.grade.service.StudentGradeQueryService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StudentGradeControllerTest {

    @Test
    void returnsStudentGradesWithGlobalResponse() {
        StudentGradeQueryService service = mock(StudentGradeQueryService.class);
        StudentGradeController controller = new StudentGradeController(service);
        CurrentUser student = new CurrentUser(10L, "STUDENT");
        StudentGradeSearchRequestDTO request = new StudentGradeSearchRequestDTO(
                null, null, null, null, null
        );
        when(service.getMyGrades(request, student)).thenReturn(new StudentGradeResponseDTO(
                new BigDecimal("3.75"), 6, new BigDecimal("3.75"), 6, List.of()
        ));

        var response = controller.getMyGrades(request, student);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data().totalGpa()).isEqualByComparingTo("3.75");
    }
}
