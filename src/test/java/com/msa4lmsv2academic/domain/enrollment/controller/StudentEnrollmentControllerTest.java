package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.request.StudentEnrollmentSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentEnrollmentResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentEnrollmentQueryService;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class StudentEnrollmentControllerTest {

    @Test
    void returnsStudentEnrollmentsWithGlobalResponse() {
        StudentEnrollmentSearchRequestDTO request =
                new StudentEnrollmentSearchRequestDTO((short) 2026, SemesterTerm.FIRST);
        CurrentUser currentUser = new CurrentUser(2001L, "STUDENT");
        StudentEnrollmentQueryService service = mock(StudentEnrollmentQueryService.class);
        when(service.getMyEnrollments(request, currentUser)).thenReturn(List.of());
        StudentEnrollmentController controller = new StudentEnrollmentController(service);

        ResponseEntity<GlobalRes<List<StudentEnrollmentResponseDTO>>> response =
                controller.getMyEnrollments(request, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEmpty();
    }
}
