package com.msa4lmsv2academic.domain.leaverequest.service;

import jakarta.servlet.http.HttpServletRequest;

public record LeaveAuditContext(String requestId, String ipAddress) {
    public LeaveAuditContext {
        requestId = bounded(requestId, 50);
        ipAddress = bounded(ipAddress, 45);
    }

    public static LeaveAuditContext from(HttpServletRequest request) {
        return new LeaveAuditContext(request.getHeader("X-Request-Id"), request.getRemoteAddr());
    }

    private static String bounded(String text, int max) {
        if (text == null || text.isBlank()) return null;
        return text.substring(0, Math.min(max, text.length()));
    }
}
