package com.msa4lmsv2academic.domain.lecture.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.request.ProfessorLectureSearchRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.ProfessorLectureResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.ProfessorLectureQueryService;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.response.PageResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ProfessorLectureControllerTest {

    @Test
    void returnsProfessorLecturesWithGlobalResponse() {
        ProfessorLectureSearchRequestDTO request = new ProfessorLectureSearchRequestDTO(
                1,
                20,
                (short) 2026,
                SemesterTerm.FIRST,
                null
        );
        CurrentUser currentUser = new CurrentUser(3001L, "PROFESSOR");
        PageResponseDTO<ProfessorLectureResponseDTO> page =
                new PageResponseDTO<>(List.of(), 0L, 1, 20, false);
        ProfessorLectureQueryService service = mock(ProfessorLectureQueryService.class);
        when(service.getMyLectures(request, currentUser)).thenReturn(page);
        ProfessorLectureController controller = new ProfessorLectureController(service);

        ResponseEntity<GlobalResponseDTO<PageResponseDTO<ProfessorLectureResponseDTO>>> response =
                controller.getMyLectures(request, currentUser);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
        assertThat(response.getBody().data()).isEqualTo(page);
    }
}
