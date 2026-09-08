package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.transfer.request.DepartmentTransferCreateRequestDTO;
import com.msa4lmsv2academic.domain.transfer.response.DepartmentTransferResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceDownload;
import com.msa4lmsv2academic.global.file.FileStorageException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
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
    private final DepartmentTransferFileValidator validator;
    private final FileStorageService storage;

    public DepartmentTransferResponseDTO create(DepartmentTransferCreateRequestDTO body,
                                                List<MultipartFile> files, String key, CurrentUser actor,
                                                DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
        List<MultipartFile> validatedFiles = validator.validate(files);
        String hash = requestHash(body, validatedFiles);
        var replay = service.preflight(body, key, hash, actor);
        if (replay.isPresent()) return replay.orElseThrow();

        List<StoredTransferDocument> uploaded = new ArrayList<>();
        try {
            for (int index = 0; index < validatedFiles.size(); index++) {
                MultipartFile file = validatedFiles.get(index);
                String storedName = storage.upload(
                        "department-transfer-requests/" + actor.id() + "/file-" + (index + 1), file);
                uploaded.add(new StoredTransferDocument(file.getOriginalFilename(), storedName,
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

    public EvidenceDownload download(Long id, Long fileId, CurrentUser actor) {
        StoredTransferDocument document = service.document(id, fileId, actor);
        return new EvidenceDownload(document.originalName(), storage.download(document.storedName()),
                document.contentType());
    }

    public DepartmentTransferResponseDTO apply(Long id, List<MultipartFile> files, String key, CurrentUser actor,
                                               DepartmentTransferAuditContext context) {
        policy.requireRole(actor, "ADMIN");
        policy.requireId(id);
        idempotency.validateKey(key);
        List<MultipartFile> validatedFiles = validator.validate(files);
        String hash = requestHash(Map.of("operation", "APPLY"), validatedFiles);
        var replay = service.preflightApplication(id, key, hash, actor);
        if (replay.isPresent()) {
            return replay.orElseThrow();
        }

        List<StoredTransferDocument> uploaded = new ArrayList<>();
        try {
            for (int index = 0; index < validatedFiles.size(); index++) {
                MultipartFile file = validatedFiles.get(index);
                String storedName = storage.upload(
                        "department-transfer-requests/" + id + "/dean-stamped-file-" + (index + 1), file);
                uploaded.add(new StoredTransferDocument(file.getOriginalFilename(), storedName,
                        file.getContentType(), file.getSize()));
            }
            AcademicChangeApplicationResult<DepartmentTransferResponseDTO> result =
                    service.apply(id, uploaded, key, hash, actor, context);
            if (!result.applied()) {
                cleanup(uploaded);
            } else {
                cleanupStoredNames(result.replacedStoredNames());
            }
            return result.response();
        } catch (RuntimeException exception) {
            cleanup(uploaded);
            throw exception;
        }
    }

    private String requestHash(Object body, List<MultipartFile> files) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", body);
        List<Map<String, Object>> fileMetadata = new ArrayList<>();
        for (MultipartFile file : files) {
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("size", file.getSize());
            try {
                metadata.put("sha256", idempotency.digest(file.getBytes()));
            } catch (IOException exception) {
                throw new FileStorageException("전과 제출 서류를 읽을 수 없습니다.", exception);
            }
            fileMetadata.add(metadata);
        }
        fileMetadata.sort(Comparator.comparing(item -> item.get("sha256").toString()));
        payload.put("files", fileMetadata);
        return idempotency.hash(payload);
    }

    private void cleanupStoredNames(List<String> storedNames) {
        for (String storedName : storedNames) {
            try {
                storage.delete(storedName);
            } catch (RuntimeException cleanupFailure) {
                log.error("전과 날인본 교체 후 이전 MinIO 객체 삭제에 실패했습니다. objectKey={}",
                        storedName, cleanupFailure);
            }
        }
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
}
