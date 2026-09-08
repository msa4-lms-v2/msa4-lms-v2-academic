package com.msa4lmsv2academic.global.websocket;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final NotificationSocketSessions sessions;
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        var command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            var attributes = accessor.getSessionAttributes();
            if (attributes == null || !(attributes.get("notificationUserId") instanceof String id)
                    || !(attributes.get("notificationRole") instanceof String role)
                    || !(attributes.get("notificationExpiry") instanceof java.time.Instant expiry)
                    || !expiry.isAfter(java.time.Instant.now())) {
                throw new AccessDeniedException("Gateway 연결 인증이 필요합니다.");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(id, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            sessions.authenticate(accessor.getSessionId(), expiry);
        } else if (command != StompCommand.DISCONNECT) {
            if (accessor.getUser() == null || !sessions.isAuthenticated(accessor.getSessionId())) {
                throw new AccessDeniedException("알림 연결 인증이 필요합니다.");
            }
            if (command == StompCommand.SEND || (command == StompCommand.SUBSCRIBE
                    && !"/user/queue/notifications".equals(accessor.getDestination()))) {
                throw new AccessDeniedException("허용되지 않은 알림 요청입니다.");
            }
        }
        return message;
    }
}
