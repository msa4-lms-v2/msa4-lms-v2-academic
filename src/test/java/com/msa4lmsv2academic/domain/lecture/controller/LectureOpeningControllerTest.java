package com.msa4lmsv2academic.domain.lecture.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.entity.LectureDayOfWeek;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningCreateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.request.LectureOpeningScheduleRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureOpeningResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.LectureOpeningService;
import com.msa4lmsv2academic.global.response.GlobalResponseDTO;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class LectureOpeningControllerTest {

    @Test
    void returnsCreatedResponseForProfessorOpeningRequest() {
        LectureOpeningService service = mock(LectureOpeningService.class);
        LectureOpeningController controller = new LectureOpeningController(service);
        LectureOpeningCreateRequestDTO request = createRequest();
        CurrentUser professor = new CurrentUser(9001L, "PROFESSOR");
        when(service.create(request, professor)).thenReturn(null);

        ResponseEntity<GlobalResponseDTO<LectureOpeningResponseDTO>> response = controller.create(request, professor);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("00");
    }

    private LectureOpeningCreateRequestDTO createRequest() {
        return new LectureOpeningCreateRequestDTO(
                31L,
                5L,
                "01",
                40,
                "공학관 301호",
                30,
                30,
                30,
                10,
                "운영체제 강의계획서",
                List.of(new LectureOpeningScheduleRequestDTO(
                        LectureDayOfWeek.MON,
                        (byte) 1,
                        (byte) 2
                ))
        );
    }
}
