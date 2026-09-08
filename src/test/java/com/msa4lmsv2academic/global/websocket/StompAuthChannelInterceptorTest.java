package com.msa4lmsv2academic.global.websocket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.WebSocketSession;

class StompAuthChannelInterceptorTest {
    @Test
    void connectCannotUseClientProvidedJwtOrUserHeaders() {
        var interceptor = new StompAuthChannelInterceptor(new NotificationSocketSessions());
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setSessionId("s");
        connect.setNativeHeader("Authorization", "Bearer anything");
        connect.setNativeHeader("X-User-Id", "42");
        assertThatThrownBy(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), null))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test
    void rejectsUnauthenticatedSubscription() {
        var interceptor = new StompAuthChannelInterceptor(new NotificationSocketSessions());
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setSessionId("s");
        headers.setDestination("/user/queue/notifications");
        assertThatThrownBy(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void authenticatedUserCannotSubscribeDirectQueueOrSend() {
        var sessions = new NotificationSocketSessions();
        var socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("s");
        sessions.open(socket);
        var interceptor = new StompAuthChannelInterceptor(sessions);
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setSessionId("s");
        connect.setSessionAttributes(java.util.Map.of("notificationUserId", "42", "notificationRole", "STUDENT",
                "notificationExpiry", Instant.now().plusSeconds(60)));
        connect.setLeaveMutable(true);
        interceptor.preSend(MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()), null);
        assertThat(connect.getUser().getName()).isEqualTo("42");
        for (var command : new StompCommand[]{StompCommand.SUBSCRIBE, StompCommand.SEND}) {
            var headers = StompHeaderAccessor.create(command);
            headers.setSessionId("s");
            headers.setUser(connect.getUser());
            headers.setDestination("/queue/notifications");
            assertThatThrownBy(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
        var own = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        own.setSessionId("s"); own.setUser(connect.getUser()); own.setDestination("/user/queue/notifications");
        assertThatCode(() -> interceptor.preSend(MessageBuilder.createMessage(new byte[0], own.getMessageHeaders()), null))
                .doesNotThrowAnyException();
    }

    @Test
    void expiredSessionIsClosed() throws Exception {
        var sessions = new NotificationSocketSessions();
        var socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("s");
        sessions.open(socket);
        sessions.authenticate("s", Instant.now().minusSeconds(1));
        assertThat(sessions.isAuthenticated("s")).isFalse();
        sessions.closeExpired();
        verify(socket).close(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION);
    }
}
