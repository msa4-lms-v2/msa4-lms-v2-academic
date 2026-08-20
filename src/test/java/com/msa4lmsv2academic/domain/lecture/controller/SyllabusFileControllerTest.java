package com.msa4lmsv2academic.domain.lecture.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadResponseDTO;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileResponseDTO;
import com.msa4lmsv2academic.domain.lecture.service.SyllabusFileService;
import com.msa4lmsv2academic.global.response.GlobalRes;
import com.msa4lmsv2academic.global.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

class SyllabusFileControllerTest {

    @Test
    void returnsCreatedResponseForUpload() {
        SyllabusFileService service = mock(SyllabusFileService.class);
        SyllabusFileController controller = new SyllabusFileController(service);
        MultipartFile file = mock(MultipartFile.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        CurrentUser professor = new CurrentUser(9101L, "PROFESSOR");
        SyllabusFileResponseDTO result = new SyllabusFileResponseDTO(
                501L, 101L, "강의계획서.pdf", "application/pdf", 1024L, 9101L,
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(service.upload(101L, file, professor, "request-1", "127.0.0.1"))
                .thenReturn(result);

        ResponseEntity<GlobalRes<SyllabusFileResponseDTO>> response = controller.upload(
                101L, file, professor, "request-1", servletRequest
        );

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(result);
    }

    @Test
    void returnsPdfWithAttachmentHeader() {
        SyllabusFileService service = mock(SyllabusFileService.class);
        SyllabusFileController controller = new SyllabusFileController(service);
        CurrentUser admin = new CurrentUser(9201L, "ADMIN");
        when(service.download(501L, admin)).thenReturn(
                new SyllabusFileDownloadResponseDTO(
                        "강의계획서.pdf",
                        "application/pdf",
                        new byte[]{1, 2, 3}
                )
        );

        ResponseEntity<byte[]> response = controller.download(501L, admin);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getContentDisposition().isAttachment()).isTrue();
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }
}
