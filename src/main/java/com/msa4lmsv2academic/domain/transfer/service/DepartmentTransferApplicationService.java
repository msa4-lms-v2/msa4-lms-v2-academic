package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;
import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferCreateRequestDTO;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.global.file.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentTransferApplicationService {
    private final DepartmentTransferService service;
    private final DepartmentTransferPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final EvidenceFileValidator validator;
    private final FileStorageService storage;

    public DepartmentTransferResponseDTO create(DepartmentTransferCreateRequestDTO body,
                                                MultipartFile selfIntroduction,
                                                MultipartFile studyPlan,
                                                String key,
                                                CurrentUser actor,
                                                DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
        validator.validateRequired(selfIntroduction);
        validator.validateRequired(studyPlan);
        List<TypedFile> files = List.of(
                new TypedFile(TransferDocumentType.SELF_INTRODUCTION, selfIntroduction),
                new TypedFile(TransferDocumentType.STUDY_PLAN, studyPlan));
        String hash = requestHash(body, files);
        var replay = service.preflight(body, key, hash, actor);
        if (replay.isPresent()) return replay.orElseThrow();

        List<StoredTransferDocument> uploaded = new ArrayList<>();
        try {
            for (TypedFile item : files) {
                MultipartFile file = item.file();
                String storedName = storage.uploadEvidence(
                        "department-transfer-requests/" + actor.id() + "/" + item.type().name().toLowerCase(), file);
                uploaded.add(new StoredTransferDocument(item.type(), file.getOriginalFilename(), storedName,
                        file.getContentType(), file.getSize()));
            }
            DepartmentTransferCreationResult result = service.create(body, uploaded, key, hash, actor, context);
            if (!result.created()) cleanup(uploaded);
            return result.response();
        } catch (RuntimeException exception) {
            cleanup(uploaded);
            throw exception;
        }
    }

    public EvidenceDownload download(Long id, TransferDocumentType documentType, CurrentUser actor) {
        StoredTransferDocument document = service.document(id, documentType, actor);
        return new EvidenceDownload(document.originalName(), storage.download(document.storedName()));
    }

    private String requestHash(DepartmentTransferCreateRequestDTO body, List<TypedFile> files) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", body);
        for (TypedFile item : files) {
            MultipartFile file = item.file();
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("size", file.getSize());
            try {
                metadata.put("sha256", idempotency.digest(file.getBytes()));
            } catch (IOException exception) {
                throw new FileStorageException("전과 제출 서류를 읽을 수 없습니다.", exception);
            }
            payload.put(item.type().name(), metadata);
        }
        return idempotency.hash(payload);
    }

    private void cleanup(List<StoredTransferDocument> uploaded) {
        for (StoredTransferDocument document : uploaded) {
            try {
                storage.delete(document.storedName());
            } catch (RuntimeException cleanupFailure) {
                log.error("전과 신청 실패 후 MinIO 보상 삭제에 실패했습니다. objectKey={}",
                        document.storedName(), cleanupFailure);
            }
        }
    }

    private record TypedFile(TransferDocumentType type, MultipartFile file) { }
}
