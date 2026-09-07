package com.msa4lmsv2academic.domain.doublemajor.service;

import com.msa4lmsv2academic.domain.doublemajor.request.DoubleMajorCreateRequestDTO;
import com.msa4lmsv2academic.domain.doublemajor.response.DoubleMajorResponseDTO;
import com.msa4lmsv2academic.domain.transfer.service.*;
import com.msa4lmsv2academic.global.file.*;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoubleMajorApplicationService {
    private final DoubleMajorService service;
    private final DoubleMajorPolicy policy;
    private final DepartmentTransferIdempotencyService idempotency;
    private final EvidenceFileValidator validator;
    private final FileStorageService storage;

    public DoubleMajorResponseDTO create(DoubleMajorCreateRequestDTO body,
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
        List<MultipartFile> files = List.of(selfIntroduction, studyPlan);
        String hash = requestHash(body, files);
        var replay = service.preflight(body, key, hash, actor);
        if (replay.isPresent()) return replay.orElseThrow();

        List<StoredTransferDocument> uploaded = new ArrayList<>();
        try {
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                String storedName = storage.uploadEvidence(
                        "double-major-requests/" + actor.id() + "/file-" + (index + 1), file);
                uploaded.add(new StoredTransferDocument(file.getOriginalFilename(), storedName,
                        file.getContentType(), file.getSize()));
            }
            DoubleMajorCreationResult result = service.create(body, uploaded, key, hash, actor, context);
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

    private String requestHash(DoubleMajorCreateRequestDTO body, List<MultipartFile> files) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", body);
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("size", file.getSize());
            try {
                metadata.put("sha256", idempotency.digest(file.getBytes()));
            } catch (IOException exception) {
                throw new FileStorageException("복수전공 제출 서류를 읽을 수 없습니다.", exception);
            }
            payload.put("file" + (index + 1), metadata);
        }
        return idempotency.hash(payload);
    }

    private void cleanup(List<StoredTransferDocument> uploaded) {
        for (StoredTransferDocument document : uploaded) {
            try {
                storage.delete(document.storedName());
            } catch (RuntimeException cleanupFailure) {
                log.error("복수전공 신청 실패 후 업로드 파일 보상 삭제에 실패했습니다: {}",
                        document.storedName(), cleanupFailure);
            }
        }
    }
}
