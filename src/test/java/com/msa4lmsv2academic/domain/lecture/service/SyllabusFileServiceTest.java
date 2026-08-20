package com.msa4lmsv2academic.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.msa4lmsv2academic.domain.lecture.entity.LectureStatus;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadResponseDTO;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadTarget;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class SyllabusFileServiceTest {

    private static final CurrentUser PROFESSOR = new CurrentUser(9101L, "PROFESSOR");

    private SyllabusFileTransactionService transactionService;
    private EvidenceFileValidator pdfFileValidator;
    private FileStorageService fileStorageService;
    private SyllabusFileService service;

    @BeforeEach
    void setUp() {
        transactionService = mock(SyllabusFileTransactionService.class);
        pdfFileValidator = mock(EvidenceFileValidator.class);
        fileStorageService = mock(FileStorageService.class);
        service = new SyllabusFileService(transactionService, pdfFileValidator, fileStorageService);
    }

    @Test
    void uploadsValidatedPdfAndRegistersMetadata() {
        MultipartFile file = mock(MultipartFile.class);
        SyllabusFileResponseDTO expected = new SyllabusFileResponseDTO(
                501L,
                101L,
                "강의계획서.pdf",
                "application/pdf",
                1024L,
                9101L,
                LocalDateTime.of(2026, 8, 20, 10, 0)
        );
        when(file.getOriginalFilename()).thenReturn("C:\\fakepath\\강의계획서.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(fileStorageService.uploadEvidence("syllabus-files/101", file))
                .thenReturn("syllabus-files/101/file.pdf");
        when(transactionService.register(
                101L,
                "강의계획서.pdf",
                "syllabus-files/101/file.pdf",
                "application/pdf",
                1024L,
                PROFESSOR,
                "request-1",
                "127.0.0.1"
        )).thenReturn(expected);

        SyllabusFileResponseDTO result = service.upload(
                101L, file, PROFESSOR, "request-1", "127.0.0.1"
        );

        assertThat(result).isEqualTo(expected);
        verify(pdfFileValidator).validateRequired(file);
        verify(transactionService).validateUploadTarget(101L, "강의계획서.pdf", 1024L, PROFESSOR);
    }

    @Test
    void removesUploadedObjectWhenDatabaseRegistrationFails() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("강의계획서.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");
        when(fileStorageService.uploadEvidence("syllabus-files/101", file))
                .thenReturn("syllabus-files/101/orphan.pdf");
        when(transactionService.register(
                101L,
                "강의계획서.pdf",
                "syllabus-files/101/orphan.pdf",
                "application/pdf",
                1024L,
                PROFESSOR,
                null,
                null
        )).thenThrow(new IllegalStateException("DB 등록 실패"));

        assertThatThrownBy(() -> service.upload(101L, file, PROFESSOR, null, null))
                .isInstanceOf(IllegalStateException.class);
        verify(fileStorageService).delete("syllabus-files/101/orphan.pdf");
    }

    @Test
    void downloadsAuthorizedFileFromStorage() {
        when(transactionService.getDownloadTarget(501L, PROFESSOR)).thenReturn(
                new SyllabusFileDownloadTarget(
                        "강의계획서.pdf",
                        "syllabus-files/101/file.pdf",
                        "application/pdf"
                )
        );
        when(fileStorageService.download("syllabus-files/101/file.pdf"))
                .thenReturn(new byte[]{1, 2, 3});

        SyllabusFileDownloadResponseDTO result = service.download(501L, PROFESSOR);

        assertThat(result.originalName()).isEqualTo("강의계획서.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).containsExactly(1, 2, 3);
    }
}
