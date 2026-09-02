package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentDownloadTarget;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceDownload;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ExcuseAttachmentService {

    private static final String STORAGE_PREFIX = "excuse-requests";

    private final ExcuseAttachmentTransactionService transactionService;
    private final EvidenceFileValidator fileValidator;
    private final FileStorageService fileStorageService;

    public ExcuseAttachmentResponseDTO upload(
            Long requestId,
            MultipartFile file,
            CurrentUser currentUser,
            String requestTraceId,
            String ipAddress
    ) {
        fileValidator.validateRequired(file);
        transactionService.validateUploadTarget(requestId, currentUser);

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String storedName = fileStorageService.uploadEvidence(STORAGE_PREFIX + "/" + requestId, file);
        try {
            return transactionService.register(
                    requestId,
                    originalName,
                    storedName,
                    file.getContentType(),
                    file.getSize(),
                    currentUser,
                    requestTraceId,
                    ipAddress
            );
        } catch (RuntimeException exception) {
            cleanupUploadedObject(storedName, exception);
            throw exception;
        }
    }

    public EvidenceDownload download(Long requestId, CurrentUser currentUser) {
        ExcuseAttachmentDownloadTarget target = transactionService.getDownloadTarget(requestId, currentUser);
        return new EvidenceDownload(target.originalName(), fileStorageService.download(target.storedName()));
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
