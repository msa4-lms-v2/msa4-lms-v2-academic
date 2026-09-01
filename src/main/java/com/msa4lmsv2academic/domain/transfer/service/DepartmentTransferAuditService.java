package com.msa4lmsv2academic.domain.transfer.service;

import com.msa4lmsv2academic.domain.audit.service.AuditLogService;
import com.msa4lmsv2academic.domain.student.entity.Student;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequest;
import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestPeriod;
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
public class DepartmentTransferAuditService {
    private final AuditLogService auditLogService;

    public Map<String, Object> snapshot(AcademicChangeRequest request) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("studentId", request.getStudent().getId());
        value.put("requestType", request.getRequestType());
        value.put("sourceDepartmentId", request.getSourceDepartment().getId());
        value.put("sourceMajorId", request.getSourceMajor() == null ? null : request.getSourceMajor().getId());
        value.put("targetDepartmentId", request.getTargetDepartment().getId());
        value.put("targetMajorId", request.getTargetMajor().getId());
        value.put("targetSemesterId", request.getTargetSemester().getId());
        value.put("status", request.getStatus());
        value.put("rejectReason", request.getRejectReason());
        value.put("processedBy", request.getProcessedBy() == null ? null : request.getProcessedBy().getId());
        value.put("processedAt", request.getProcessedAt());
        value.put("cancelReason", request.getCancelReason());
        value.put("cancelledBy", request.getCancelledBy() == null ? null : request.getCancelledBy().getId());
        value.put("cancelledAt", request.getCancelledAt());
        value.put("documents", request.getFiles().stream().map(file -> Map.of(
                "documentType", file.getDocumentType(),
                "originalName", file.getOriginalName(),
                "storedName", file.getStoredName(),
                "contentType", file.getContentType(),
                "size", file.getSize())).toList());
        return value;
    }

    public Map<String, Object> affiliation(Student student) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("departmentId", student.getDepartment().getId());
        value.put("majorId", student.getMajor() == null ? null : student.getMajor().getId());
        value.put("doubleMajorId", student.getDoubleMajor() == null ? null : student.getDoubleMajor().getId());
        value.put("advisorId", student.getAdvisor() == null ? null : student.getAdvisor().getId());
        return value;
    }

    public Map<String, Object> snapshot(AcademicChangeRequestPeriod period) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("semesterId", period.getSemester().getId());
        value.put("requestType", period.getRequestType());
        value.put("startAt", period.getStartAt());
        value.put("endAt", period.getEndAt());
        value.put("active", period.isActive());
        return value;
    }

    public void record(Long id, String targetType, Map<String, Object> before, Map<String, Object> after,
                       String action, String reason, CurrentUser actor, DepartmentTransferAuditContext context) {
        auditLogService.record(actor.id(), action, targetType, id, before, after, reason,
                context.requestId(), context.ipAddress());
    }
}
