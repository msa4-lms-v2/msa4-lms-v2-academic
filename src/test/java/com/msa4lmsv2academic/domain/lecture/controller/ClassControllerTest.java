package com.msa4lmsv2academic.domain.lecture.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.request.StudentClassSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.StudentClassResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.StudentClassQueryService;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ClassControllerTest {

    @Test
    void returnsStudentClassesWithGlobalResponse() {
        StudentClassSearchRequestDTO request =
                new StudentClassSearchRequestDTO((short) 2026, SemesterTerm.FIRST);
        CurrentUser currentUser = new CurrentUser(2001L, "STUDENT");
        StudentClassQueryService service = mock(StudentClassQueryService.class);
        when(service.getMyClasses(request, currentUser)).thenReturn(List.of());
        ClassController controller = new ClassController(service);

        ResponseEntity<GlobalRes<List<StudentClassResponseDTO>>> response =
                controller.getMyClasses(request, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEmpty();
    }
}
