package com.msa4lmsv2academic.global.websocket;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Component
public class NotificationSocketSessions {
    private final ConcurrentHashMap<String, Entry> connections = new ConcurrentHashMap<>();
    public void open(WebSocketSession session) {
        connections.put(session.getId(), new Entry(session, Instant.now().plusSeconds(10), false));
    }
    public void remove(String id) { connections.remove(id); }
    public void authenticate(String id, Instant expiry) {
        connections.computeIfPresent(id, (key, old) -> new Entry(old.session(), expiry, true));
    }
    public boolean isAuthenticated(String id) {
        var entry = connections.get(id);
        return entry != null && entry.authenticated() && Instant.now().isBefore(entry.deadline());
    }
    @Scheduled(fixedDelay = 1000)
    public void closeExpired() {
        connections.forEach((id, entry) -> {
            if (!Instant.now().isBefore(entry.deadline())) {
                try { entry.session().close(CloseStatus.POLICY_VIOLATION); }
                catch (java.io.IOException ignored) { /* Transport already closed. */ }
                finally { connections.remove(id, entry); }
            }
        });
    }
    private record Entry(WebSocketSession session, Instant deadline, boolean authenticated) {}
}
