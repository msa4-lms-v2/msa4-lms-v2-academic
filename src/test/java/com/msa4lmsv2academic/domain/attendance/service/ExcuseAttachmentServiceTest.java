package com.msa4lmsv2academic.domain.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentDownloadTarget;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class ExcuseAttachmentServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(1001L, "STUDENT");

    private ExcuseAttachmentTransactionService transactionService;
    private EvidenceFileValidator fileValidator;
    private FileStorageService fileStorageService;
    private ExcuseAttachmentService service;

    @BeforeEach
    void setUp() {
        transactionService = mock(ExcuseAttachmentTransactionService.class);
        fileValidator = mock(EvidenceFileValidator.class);
        fileStorageService = mock(FileStorageService.class);
        service = new ExcuseAttachmentService(transactionService, fileValidator, fileStorageService);
    }

    @Test
    void uploadsValidatedPdfAndRegistersMetadata() {
        MultipartFile file = mock(MultipartFile.class);
        ExcuseAttachmentResponseDTO expected = new ExcuseAttachmentResponseDTO(
                31L,
                "진료확인서.pdf",
                "application/pdf",
                2048L,
                LocalDateTime.of(2026, 9, 2, 10, 30)
        );
        when(file.getOriginalFilename()).thenReturn("C:\\fakepath\\진료확인서.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(2048L);
        when(fileStorageService.uploadEvidence("excuse-requests/31", file))
                .thenReturn("excuse-requests/31/file.pdf");
        when(transactionService.register(
                31L,
                "진료확인서.pdf",
                "excuse-requests/31/file.pdf",
                "application/pdf",
                2048L,
                STUDENT,
                "request-1",
                "127.0.0.1"
        )).thenReturn(expected);

        ExcuseAttachmentResponseDTO result = service.upload(
                31L, file, STUDENT, "request-1", "127.0.0.1"
        );

        assertThat(result).isEqualTo(expected);
        verify(fileValidator).validateRequired(file);
        verify(transactionService).validateUploadTarget(31L, STUDENT);
    }

    @Test
    void removesNewMinioObjectWhenDatabaseRegistrationFails() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("진료확인서.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(2048L);
        when(fileStorageService.uploadEvidence("excuse-requests/31", file))
                .thenReturn("excuse-requests/31/orphan.pdf");
        when(transactionService.register(
                31L,
                "진료확인서.pdf",
                "excuse-requests/31/orphan.pdf",
                "application/pdf",
                2048L,
                STUDENT,
                null,
                null
        )).thenThrow(new IllegalStateException("DB 등록 실패"));

        assertThatThrownBy(() -> service.upload(31L, file, STUDENT, null, null))
                .isInstanceOf(IllegalStateException.class);
        verify(fileStorageService).delete("excuse-requests/31/orphan.pdf");
    }

    @Test
    void downloadsOnlyAfterTransactionServiceAuthorizesTarget() {
        when(transactionService.getDownloadTarget(31L, STUDENT)).thenReturn(
                new ExcuseAttachmentDownloadTarget(
                        "진료확인서.pdf",
                        "excuse-requests/31/file.pdf"
                )
        );
        when(fileStorageService.download("excuse-requests/31/file.pdf"))
                .thenReturn(new byte[]{1, 2, 3});

        var result = service.download(31L, STUDENT);

        assertThat(result.originalName()).isEqualTo("진료확인서.pdf");
        assertThat(result.content()).containsExactly(1, 2, 3);
    }
}
