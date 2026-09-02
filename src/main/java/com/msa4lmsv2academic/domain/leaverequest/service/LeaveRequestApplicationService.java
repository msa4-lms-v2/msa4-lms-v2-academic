package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.leaverequest.request.LeaveRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;
import com.msa4lmsv2academic.global.file.EvidenceFileValidator;
import com.msa4lmsv2academic.global.file.FileStorageException;
import com.msa4lmsv2academic.global.file.FileStorageService;
import com.msa4lmsv2academic.global.error.InvalidFileException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestApplicationService {
    private static final int MAX_FILE_COUNT = 5;
    private final LeaveRequestService service;
    private final LeaveRequestPolicy policy;
    private final LeaveIdempotencyService idempotency;
    private final EvidenceFileValidator fileValidator;
    private final FileStorageService storage;

    // MinIO I/O를 DB transaction 밖에 둡니다. 업로드 이후 변경된 업무 조건은 쓰기 서비스에서 재검증합니다.
    public LeaveRequestResponseDTO create(LeaveRequestCreateRequestDTO body, List<MultipartFile> files, String key,
                                          CurrentUser actor, LeaveAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
        List<MultipartFile> attachments = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (attachments.size() > MAX_FILE_COUNT) {
            throw new InvalidFileException("증빙 파일은 최대 5개까지 첨부할 수 있습니다.");
        }
        if (body.requestType() == LeaveRequestType.MILITARY_LEAVE && attachments.size() != 1) {
            throw new InvalidFileException("군휴학에는 입영통지서 PDF 1개가 필수입니다.");
        }
        attachments.forEach(fileValidator::validateRequired);
        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", body);
        List<Map<String, Object>> filePayload = new ArrayList<>();
        for (MultipartFile file : attachments) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("contentType", file.getContentType());
            metadata.put("size", file.getSize());
            try {
                metadata.put("sha256", idempotency.digest(file.getBytes()));
            } catch (IOException exception) {
                throw new FileStorageException("증빙 파일을 읽을 수 없습니다.", exception);
            }
            filePayload.add(metadata);
        }
        payload.put("files", filePayload);
        String hash = idempotency.hash(payload);
        var replay = service.preflight(body, key, hash, actor);
        if (replay.isPresent()) return replay.orElseThrow();

        List<LeaveAttachment> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : attachments) {
                String objectKey = storage.uploadEvidence("leave-requests/" + actor.id(), file);
                uploaded.add(new LeaveAttachment(file.getOriginalFilename(), objectKey, file.getContentType(), file.getSize()));
            }
            LeaveRequestCreationResult result = service.create(body, uploaded, key, hash, actor, context);
            if (!result.created()) cleanup(uploaded);
            return result.response();
        } catch (RuntimeException exception) {
            cleanup(uploaded);
            throw exception;
        }
    }

    public Download download(Long id, Long fileId, CurrentUser actor) {
        LeaveAttachment attachment = service.attachment(id, fileId, actor);
        return new Download(attachment.originalName(), storage.download(attachment.storedName()));
    }

    public Download downloadFirst(Long id, CurrentUser actor) {
        LeaveAttachment attachment = service.firstAttachment(id, actor);
        return new Download(attachment.originalName(), storage.download(attachment.storedName()));
    }

    private void cleanup(List<LeaveAttachment> uploaded) {
        for (LeaveAttachment file : uploaded) {
            try {
                storage.delete(file.storedName());
            } catch (RuntimeException cleanupFailure) {
                log.error("휴·복학 신청 실패 후 MinIO 보상 삭제에 실패했습니다. objectKey={}",
                        file.storedName(), cleanupFailure);
            }
        }
    }

    public record Download(String filename, byte[] bytes) { }
}
