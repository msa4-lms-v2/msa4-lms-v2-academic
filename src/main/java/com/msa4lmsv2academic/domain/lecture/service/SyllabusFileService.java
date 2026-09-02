package com.msa4lmsv2academic.domain.lecture.service;

import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadResponseDTO;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileDownloadTarget;
import com.msa4lmsv2academic.domain.lecture.response.SyllabusFileResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SyllabusFileService {

    private static final String STORAGE_PREFIX = "syllabus-files";

    private final SyllabusFileTransactionService transactionService;
    private final EvidenceFileValidator pdfFileValidator;
    private final FileStorageService fileStorageService;

    public SyllabusFileResponseDTO upload(
            Long classId,
            MultipartFile file,
            CurrentUser currentUser,
            String requestId,
            String ipAddress
    ) {
        pdfFileValidator.validateRequired(file);
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        transactionService.validateUploadTarget(classId, originalName, file.getSize(), currentUser);

        String storedName = fileStorageService.uploadEvidence(STORAGE_PREFIX + "/" + classId, file);
        try {
            return transactionService.register(
                    classId,
                    originalName,
                    storedName,
                    file.getContentType(),
                    file.getSize(),
                    currentUser,
                    requestId,
                    ipAddress
            );
        } catch (RuntimeException exception) {
            cleanupUploadedObject(storedName, exception);
            throw exception;
        }
    }

    public List<SyllabusFileResponseDTO> list(Long classId, CurrentUser currentUser) {
        return transactionService.list(classId, currentUser);
    }

    public SyllabusFileDownloadResponseDTO download(Long fileId, CurrentUser currentUser) {
        SyllabusFileDownloadTarget target = transactionService.getDownloadTarget(fileId, currentUser);
        byte[] content = fileStorageService.download(target.storedName());
        return new SyllabusFileDownloadResponseDTO(
                target.originalName(),
                target.contentType(),
                content
        );
    }

    private String normalizeOriginalName(String originalName) {
        int slashIndex = Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\'));
        return slashIndex < 0 ? originalName : originalName.substring(slashIndex + 1);
    }

    private void cleanupUploadedObject(String storedName, RuntimeException originalException) {
        try {
            fileStorageService.delete(storedName);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
