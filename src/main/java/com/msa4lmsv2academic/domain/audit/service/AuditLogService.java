package com.msa4lmsv2academic.domain.audit.service;

import com.msa4lmsv2academic.domain.audit.entity.AuditLog;
import com.msa4lmsv2academic.domain.audit.repository.AuditLogRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, String action, String targetType, Long targetId,
                       Map<String, Object> beforeValue, Map<String, Object> afterValue,
                       String reason, String requestId, String ipAddress) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                action,
                targetType,
                targetId,
                beforeValue,
                afterValue,
                reason,
                requestId,
                ipAddress
        ));
    }
}
