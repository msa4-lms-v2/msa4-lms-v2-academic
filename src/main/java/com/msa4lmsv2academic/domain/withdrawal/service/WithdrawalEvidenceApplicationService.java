package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.withdrawal.request.WithdrawalAttachmentUpdateRequestDTO;
import com.msa4lmsv2academic.domain.withdrawal.response.WithdrawalResponseDTO;
import com.msa4lmsv2academic.global.error.WithdrawalAccessDeniedException;
import com.msa4lmsv2academic.global.file.EvidenceDownload;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.io.IOException;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WithdrawalEvidenceApplicationService {
    private final WithdrawalService service;
    private final WithdrawalIdempotencyService idempotency;
    private final EvidenceFileValidator fileValidator;
    private final FileStorageService storage;

    public WithdrawalResponseDTO update(Long withdrawalId, WithdrawalAttachmentUpdateRequestDTO request,
                                        MultipartFile file, String key, CurrentUser actor,
                                        WithdrawalAuditContext context) {
        requireWriter(actor);
        idempotency.validateKey(key);
        fileValidator.validateRequired(file);

        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", request);
        payload.put("filename", file.getOriginalFilename());
        payload.put("contentType", file.getContentType());
        payload.put("size", file.getSize());
        try {
            payload.put("sha256", idempotency.digest(file.getBytes()));
        } catch (IOException exception) {
            throw new FileStorageException("증빙 파일을 읽을 수 없습니다.", exception);
        }
        String hash = idempotency.hash(payload);
        var replay = service.preflightAttachmentUpdate(withdrawalId, request, key, hash, actor);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        String objectKey = storage.uploadEvidence("withdrawal-requests/" + withdrawalId, file);
        var attachment = new WithdrawalAttachment(
                file.getOriginalFilename(), objectKey, file.getContentType(), file.getSize());
        return service.updateAttachment(withdrawalId, request, attachment, key, hash, actor, context);
    }

    public EvidenceDownload download(Long withdrawalId, CurrentUser actor) {
        WithdrawalAttachment attachment = service.attachment(withdrawalId, actor);
        return new EvidenceDownload(attachment.originalName(), storage.download(attachment.storedName()));
    }

    private void requireWriter(CurrentUser actor) {
        if (actor == null || actor.id() == null
                || !("STUDENT".equals(actor.role()) || "ADMIN".equals(actor.role()))) {
            throw new WithdrawalAccessDeniedException("자퇴 증빙 변경 권한이 없습니다.");
        }
    }
}
