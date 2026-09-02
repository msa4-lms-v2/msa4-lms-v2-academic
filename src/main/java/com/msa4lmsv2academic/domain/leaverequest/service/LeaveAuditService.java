package com.msa4lmsv2academic.domain.leaverequest.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequest;
import com.msa4lmsv2academic.domain.leaverequest.entity.LeaveRequestPeriod;
import com.msa4lmsv2academic.global.security.CurrentUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LeaveAuditService {
    private final AuditLogService auditLogService;

    public Map<String, Object> snapshot(LeaveRequest request) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("studentId", request.getStudent().getId());
        value.put("academicStatus", request.getStudent().getAcademicStatus());
        value.put("requestType", request.getRequestType());
        value.put("status", request.getStatus());
        value.put("reason", request.getReason());
        value.put("targetYear", request.getTargetYear());
        value.put("targetSemester", request.getTargetSemester());
        value.put("returnYear", request.getReturnYear());
        value.put("returnSemester", request.getReturnSemester());
        value.put("rejectReason", request.getRejectReason());
        value.put("cancelReason", request.getCancelReason());
        List<Map<String, Object>> files = request.getFiles().stream().map(file -> {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", file.getId());
            metadata.put("originalName", file.getOriginalName());
            metadata.put("storedName", file.getStoredName());
            metadata.put("contentType", file.getContentType());
            metadata.put("size", file.getSize());
            return metadata;
        }).toList();
        value.put("files", files);
        return value;
    }

    public Map<String, Object> snapshot(LeaveRequestPeriod period) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("semesterId", period.getSemester().getId());
        value.put("requestType", period.getRequestType());
        value.put("startAt", period.getStartAt());
        value.put("endAt", period.getEndAt());
        value.put("approvalStartAt", period.getApprovalStartAt());
        value.put("approvalEndAt", period.getApprovalEndAt());
        value.put("active", period.isActive());
        return value;
    }

    public void record(Long id, String target, Map<String, Object> before, Map<String, Object> after,
                       String action, String reason, CurrentUser actor, LeaveAuditContext context) {
        // 500자 사유는 snapshot 원문에 보존하고 audit.reason에는 짧은 행위 설명을 기록합니다.
        auditLogService.record(actor.id(), action, target, id, before, after, reason,
                context.requestId(), context.ipAddress());
    }
}
