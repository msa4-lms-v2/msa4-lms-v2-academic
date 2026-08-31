package com.msa4lmsv2academic.domain.transfer.service;

import jakarta.servlet.http.HttpServletRequest;

public record DepartmentTransferAuditContext(String requestId, String ipAddress) {
    public static DepartmentTransferAuditContext from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return new DepartmentTransferAuditContext(request.getHeader("X-Request-Id"), ip);
    }
}
