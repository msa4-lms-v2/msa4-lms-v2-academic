package com.msa4lmsv2academic.domain.dismissal.service;

import jakarta.servlet.http.HttpServletRequest;

public record DismissalAuditContext(String requestId, String ipAddress) {
    public DismissalAuditContext {
        requestId = limit(requestId, 50);
        ipAddress = limit(ipAddress, 45);
    }

    public static DismissalAuditContext from(HttpServletRequest request) {
        return new DismissalAuditContext(request.getHeader("X-Request-Id"), request.getRemoteAddr());
    }

    private static String limit(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }
}
