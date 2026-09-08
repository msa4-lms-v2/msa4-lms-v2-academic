package com.msa4lmsv2academic.global.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//서버에 WebSocket 연결 입구를 만들고, 연결된 사용자에게 알림을 전달할 경로 규칙을 설정하는 코드
@Configuration
@EnableWebSocketMessageBroker
@lombok.RequiredArgsConstructor
@org.springframework.scheduling.annotation.EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor authInterceptor;
    private final NotificationSocketSessions sessions;
    private final GatewayNotificationHandshake handshake;
    @org.springframework.beans.factory.annotation.Value("${NOTIFICATION_ALLOWED_ORIGINS:https://mirae.meerkat.p-e.kr}")
    private String[] allowedOrigins;

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new org.springframework.web.socket.handler.WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
                sessions.open(session);
                super.afterConnectionEstablished(session);
            }
            @Override
            public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session,
                    org.springframework.web.socket.CloseStatus status) throws Exception {
                sessions.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .addInterceptors(handshake)
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
