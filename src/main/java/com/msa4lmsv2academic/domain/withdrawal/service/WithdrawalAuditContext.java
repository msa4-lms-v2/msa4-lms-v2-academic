package com.msa4lmsv2academic.domain.withdrawal.service;

import jakarta.servlet.http.HttpServletRequest;

public record WithdrawalAuditContext(String requestId, String ipAddress) {
    public static WithdrawalAuditContext from(HttpServletRequest request) {
        // 전달 경로가 검증되지 않은 X-Forwarded-For를 실제 사용자 IP로 신뢰하지 않습니다.
        return new WithdrawalAuditContext(limit(request.getHeader("X-Request-Id"), 50),
                limit(request.getRemoteAddr(), 45));
    }

    private static String limit(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }
}

