package com.msa4lmsv2academic.domain.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.enrollment.request.StudentTimetableSearchRequestDTO;
import com.msa4lmsv2academic.domain.enrollment.response.StudentTimetableResponseDTO;
import com.msa4lmsv2academic.domain.enrollment.service.StudentTimetableQueryService;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StudentTimetableControllerTest {

    @Test
    void returnsTimetableWithGlobalResponse() {
        StudentTimetableQueryService service = mock(StudentTimetableQueryService.class);
        StudentTimetableController controller = new StudentTimetableController(service);
        StudentTimetableSearchRequestDTO request =
                new StudentTimetableSearchRequestDTO((short) 2026, SemesterTerm.FIRST);
        CurrentUser currentUser = new CurrentUser(93101L, "STUDENT");
        when(service.getMyTimetable(request, currentUser)).thenReturn(
                new StudentTimetableResponseDTO((short) 2026, SemesterTerm.FIRST, 0, List.of())
        );

        var response = controller.getMyTimetable(request, currentUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data().totalCredits()).isZero();
        assertThat(response.getBody().data().items()).isEmpty();
    }
}
