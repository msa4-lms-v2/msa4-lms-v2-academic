package com.msa4lmsv2academic.domain.lecture.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.request.LectureSyllabusUpdateRequestDTO;
import com.msa4lmsv2academic.domain.lecture.response.LectureSyllabusResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.LectureSyllabusService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class LectureSyllabusControllerTest {

    @Test
    void returnsSyllabusUpdateResult() {
        LectureSyllabusService service = mock(LectureSyllabusService.class);
        LectureSyllabusController controller = new LectureSyllabusController(service);
        LectureSyllabusUpdateRequestDTO request = new LectureSyllabusUpdateRequestDTO("강의계획서");
        CurrentUser professor = new CurrentUser(9101L, "PROFESSOR");
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        LectureSyllabusResponseDTO result = new LectureSyllabusResponseDTO(
                101L, "강의계획서", LectureStatus.OPEN
        );
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.update(101L, request, professor, "request-1", "127.0.0.1"))
                .thenReturn(result);

        ResponseEntity<GlobalRes<LectureSyllabusResponseDTO>> response = controller.update(
                101L, request, professor, "request-1", servletRequest
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(result);
    }
}
