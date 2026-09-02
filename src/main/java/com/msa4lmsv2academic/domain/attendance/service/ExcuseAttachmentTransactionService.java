package com.msa4lmsv2academic.domain.attendance.service;

import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequest;
import com.msa4lmsv2academic.domain.attendance.entity.ExcuseRequestStatus;
import com.msa4lmsv2academic.domain.attendance.repository.ExcuseRequestRepository;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentDownloadTarget;
import com.msa4lmsv2academic.domain.attendance.response.ExcuseAttachmentResponseDTO;
import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.global.error.ExcuseAttachmentConflictException;
import com.msa4lmsv2academic.global.error.ExcuseAttachmentNotFoundException;
import com.msa4lmsv2academic.global.error.ExcuseRequestAccessDeniedException;
import com.msa4lmsv2academic.global.error.ExcuseRequestNotFoundException;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcuseAttachmentTransactionService {

    private static final String AUDIT_TARGET_TYPE = "EXCUSE_REQUEST";

    private final ExcuseRequestRepository excuseRequestRepository;
    private final AuditLogService auditLogService;

    public void validateUploadTarget(Long requestId, CurrentUser currentUser) {
        validateStudent(currentUser);
        ExcuseRequest excuseRequest = findDetail(requestId);
        validateStudentOwner(excuseRequest, currentUser);
        validatePending(excuseRequest);
    }

    @Transactional
    public ExcuseAttachmentResponseDTO register(
            Long requestId,
            String originalName,
            String storedName,
            String contentType,
            long size,
            CurrentUser currentUser,
            String requestTraceId,
            String ipAddress
    ) {
        validateStudent(currentUser);
        ExcuseRequest excuseRequest = excuseRequestRepository.findDetailForUpdate(requestId)
                .orElseThrow(ExcuseRequestNotFoundException::new);
        validateStudentOwner(excuseRequest, currentUser);
        validatePending(excuseRequest);

        Map<String, Object> beforeValue = attachmentSnapshot(excuseRequest);
        try {
            excuseRequest.replaceAttachment(originalName, storedName, contentType, size);
        } catch (IllegalStateException exception) {
            throw new ExcuseAttachmentConflictException(exception.getMessage());
        }
        excuseRequestRepository.flush();

        auditLogService.record(
                currentUser.id(),
                beforeValue.isEmpty() ? "EXCUSE_ATTACHMENT_UPLOADED" : "EXCUSE_ATTACHMENT_REPLACED",
                AUDIT_TARGET_TYPE,
                excuseRequest.getId(),
                beforeValue.isEmpty() ? null : beforeValue,
                attachmentSnapshot(excuseRequest),
                beforeValue.isEmpty() ? "공결 증빙 등록" : "공결 증빙 교체",
                normalizeNullable(requestTraceId),
                normalizeNullable(ipAddress)
        );
        return ExcuseAttachmentResponseDTO.from(excuseRequest);
    }

    public ExcuseAttachmentDownloadTarget getDownloadTarget(Long requestId, CurrentUser currentUser) {
        validateReadableRole(currentUser);
        ExcuseRequest excuseRequest = findDetail(requestId);
        validateReadable(excuseRequest, currentUser);
        if (!excuseRequest.hasAttachment()) {
            throw new ExcuseAttachmentNotFoundException();
        }
        return new ExcuseAttachmentDownloadTarget(
                excuseRequest.getAttachmentOriginalName(),
                excuseRequest.getAttachmentStoredName()
        );
    }

    private ExcuseRequest findDetail(Long requestId) {
        return excuseRequestRepository.findDetailById(requestId)
                .orElseThrow(ExcuseRequestNotFoundException::new);
    }

    private void validateStudent(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!"STUDENT".equals(currentUser.role())) {
            throw new ExcuseRequestAccessDeniedException("학생만 공결 증빙을 등록하거나 교체할 수 있습니다.");
        }
    }

    private void validateReadableRole(CurrentUser currentUser) {
        validateAuthenticated(currentUser);
        if (!("STUDENT".equals(currentUser.role())
                || "PROFESSOR".equals(currentUser.role())
                || currentUser.isAdmin())) {
            throw new ExcuseRequestAccessDeniedException("공결 증빙을 조회할 권한이 없습니다.");
        }
    }

    private void validateAuthenticated(CurrentUser currentUser) {
        if (currentUser == null || currentUser.id() == null || currentUser.role() == null) {
            throw new ExcuseRequestAccessDeniedException("인증된 사용자만 공결 증빙을 사용할 수 있습니다.");
        }
    }

    private void validateStudentOwner(ExcuseRequest excuseRequest, CurrentUser currentUser) {
        Long ownerUserId = excuseRequest.getEnrollment().getStudent().getUser().getId();
        if (!ownerUserId.equals(currentUser.id())) {
            throw new ExcuseRequestAccessDeniedException("본인의 공결 신청 증빙만 변경할 수 있습니다.");
        }
    }

    private void validatePending(ExcuseRequest excuseRequest) {
        if (excuseRequest.getStatus() != ExcuseRequestStatus.PENDING) {
            throw new ExcuseAttachmentConflictException("처리 대기 상태인 공결 신청의 증빙만 변경할 수 있습니다.");
        }
    }

    private void validateReadable(ExcuseRequest excuseRequest, CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        Long allowedUserId = "STUDENT".equals(currentUser.role())
                ? excuseRequest.getEnrollment().getStudent().getUser().getId()
                : excuseRequest.getEnrollment().getLecture().getProfessor().getUser().getId();
        if (!allowedUserId.equals(currentUser.id())) {
            throw new ExcuseRequestAccessDeniedException("해당 공결 증빙을 조회할 권한이 없습니다.");
        }
    }

    private Map<String, Object> attachmentSnapshot(ExcuseRequest excuseRequest) {
        if (!excuseRequest.hasAttachment()) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("originalName", excuseRequest.getAttachmentOriginalName());
        snapshot.put("storedName", excuseRequest.getAttachmentStoredName());
        snapshot.put("contentType", excuseRequest.getAttachmentContentType());
        snapshot.put("size", excuseRequest.getAttachmentSize());
        return snapshot;
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
