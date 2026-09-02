package com.msa4lmsv2academic.domain.dismissal.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.dismissal.entity.DismissalCandidate;
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
public class DismissalAuditService {
    private final AuditLogService auditLogService;

    public Map<String, Object> snapshot(DismissalCandidate candidate) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("studentId", candidate.getStudent().getId());
        value.put("academicStatus", candidate.getStudent().getAcademicStatus());
        value.put("version", candidate.getVersion());
        value.put("reasonType", candidate.getReasonType());
        value.put("reason", candidate.getReason());
        value.put("status", candidate.getStatus());
        value.put("registeredBy", candidate.getRegisteredBy());
        value.put("processedBy", candidate.getProcessedBy());
        value.put("processedAt", candidate.getProcessedAt());
        value.put("cancelReason", candidate.getCancelReason());
        return value;
    }

    public void record(DismissalCandidate candidate, Map<String, Object> before, String action,
                       CurrentUser actor, DismissalAuditContext context) {
        // 500자 원문은 JSON에 보존하고 audit.reason에는 짧은 행위명을 기록합니다.
        auditLogService.record(actor.id(), action, "DISMISSAL", candidate.getId(), before, snapshot(candidate),
                action, context.requestId(), context.ipAddress());
    }
}
