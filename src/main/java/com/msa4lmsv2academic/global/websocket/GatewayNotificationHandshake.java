package com.msa4lmsv2academic.global.websocket;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Academic must only be reachable through the trusted Gateway (same trust as REST headers). */
@Component
public class GatewayNotificationHandshake implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Map<String, Object> attributes) {
        try {
            String id = request.getHeaders().getFirst("X-User-Id");
            String role = request.getHeaders().getFirst("X-User-Role");
            Instant expiry = Instant.ofEpochSecond(Long.parseLong(request.getHeaders().getFirst("X-User-Expires-At")));
            if (Long.parseLong(id) <= 0 || !Set.of("STUDENT", "PROFESSOR").contains(role) || !expiry.isAfter(Instant.now())) {
                throw new IllegalArgumentException();
            }
            attributes.put("notificationUserId", id);
            attributes.put("notificationRole", role);
            attributes.put("notificationExpiry", expiry);
            return true;
        } catch (RuntimeException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler handler, Exception exception) {}
}
