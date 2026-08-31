package com.msa4lmsv2academic.domain.withdrawal.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.withdrawal.entity.WithdrawalRequest;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class WithdrawalAuditService {
    private final AuditLogService auditLogService;

    public void record(WithdrawalRequest request, Map<String, Object> before, String action, String reason,
                       CurrentUser currentUser, WithdrawalAuditContext context) {
        // 신청/반려 원문(최대 500자)은 JSON에 보존하고 255자 audit.reason에는 행위 설명을 저장합니다.
        auditLogService.record(currentUser.id(), action, "WITHDRAWAL_REQUEST", request.getId(), before, snapshot(request),
                reason, context.requestId(), context.ipAddress());
    }

    public Map<String, Object> snapshot(WithdrawalRequest request) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("studentId", request.getStudent().getId());
        value.put("academicStatus", request.getStudent().getAcademicStatus().name());
        value.put("status", request.getStatus().name());
        value.put("reason", request.getReason());
        value.put("requestedEffectiveDate", request.getRequestedEffectiveDate());
        value.put("effectiveDate", request.getEffectiveDate());
        value.put("advisorReviewedBy", request.getAdvisorReviewedBy() == null ? null : request.getAdvisorReviewedBy().getId());
        value.put("advisorReviewedAt", request.getAdvisorReviewedAt());
        value.put("advisorRejectReason", request.getAdvisorRejectReason());
        value.put("processedBy", request.getProcessedBy() == null ? null : request.getProcessedBy().getId());
        value.put("processedAt", request.getProcessedAt());
        value.put("rejectReason", request.getRejectReason());
        value.put("cancelledBy", request.getCancelledBy() == null ? null : request.getCancelledBy().getId());
        value.put("cancelledAt", request.getCancelledAt());
        value.put("cancelReason", request.getCancelReason());
        value.put("attachmentOriginalName", request.getAttachmentOriginalName());
        value.put("attachmentStoredName", request.getAttachmentStoredName());
        value.put("attachmentContentType", request.getAttachmentContentType());
        value.put("attachmentSize", request.getAttachmentSize());
        return value;
    }

}
