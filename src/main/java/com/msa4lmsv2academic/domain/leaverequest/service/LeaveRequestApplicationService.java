package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestType;
import com.msa4lmsv2academic.domain.leaverequest.request.LeaveRequestCreateRequestDTO;
import com.msa4lmsv2academic.domain.leaverequest.response.LeaveRequestResponseDTO;
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
public class LeaveRequestApplicationService {
    private final LeaveRequestService service;
    private final LeaveRequestPolicy policy;
    private final LeaveIdempotencyService idempotency;
    private final EvidenceFileValidator fileValidator;
    private final FileStorageService storage;

    // MinIO I/O를 DB transaction 밖에 둡니다. 업로드 이후 변경된 업무 조건은 쓰기 서비스에서 재검증합니다.
    public LeaveRequestResponseDTO create(LeaveRequestCreateRequestDTO body, MultipartFile file, String key,
                                          CurrentUser actor, LeaveAuditContext context) {
        policy.requireRole(actor, "STUDENT");
        policy.validateCreate(body);
        idempotency.validateKey(key);
        if (body.requestType() == LeaveRequestType.MILITARY_LEAVE) fileValidator.validateRequired(file);
        else fileValidator.validateOptional(file);
        boolean attached = file != null && !file.isEmpty();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("request", body);
        if (attached) {
            payload.put("filename", file.getOriginalFilename());
            payload.put("contentType", file.getContentType());
            payload.put("size", file.getSize());
            try {
                payload.put("sha256", idempotency.digest(file.getBytes()));
            } catch (IOException exception) {
                throw new FileStorageException("증빙 파일을 읽을 수 없습니다.", exception);
            }
        }
        String hash = idempotency.hash(payload);
        var replay = service.preflight(body, key, hash, actor);
        if (replay.isPresent()) return replay.orElseThrow();
        LeaveAttachment attachment = LeaveAttachment.empty();
        if (attached) {
            String objectKey = storage.uploadEvidence("leave-requests/" + actor.id(), file);
            attachment = new LeaveAttachment(file.getOriginalFilename(), objectKey, file.getContentType(), file.getSize());
        }
        return service.create(body, attachment, key, hash, actor, context);
    }

    public Download download(Long id, CurrentUser actor) {
        LeaveAttachment attachment = service.attachment(id, actor);
        return new Download(attachment.originalName(), storage.download(attachment.storedName()));
    }

    public record Download(String filename, byte[] bytes) { }
}
